package me.dodo.readingnotes.controller;

import jakarta.servlet.http.HttpServletRequest;
import me.dodo.readingnotes.dto.common.ApiResponse;
import org.springframework.beans.factory.annotation.Value;
import me.dodo.readingnotes.dto.reflection.ClusterResult;
import me.dodo.readingnotes.dto.reflection.ComposeRequest;
import me.dodo.readingnotes.dto.reflection.ElicitRequest;
import me.dodo.readingnotes.dto.reflection.ElicitResponse;
import me.dodo.readingnotes.dto.reflection.ElicitSaveRequest;
import me.dodo.readingnotes.dto.reflection.ReflectionResponse;
import me.dodo.readingnotes.dto.reflection.ReflectionSaveRequest;
import me.dodo.readingnotes.exception.AuthException;
import me.dodo.readingnotes.service.reflection.EliciterService;
import me.dodo.readingnotes.service.reflection.ReflectionService;
import me.dodo.readingnotes.service.reflection.ReflectionStorageService;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.Map;

@RestController
@RequestMapping("/api/reflection")
public class ReflectionController {

    private final ReflectionService reflectionService;
    private final EliciterService eliciterService;
    private final ReflectionStorageService storageService;

    /** 이 기능을 쓸 수 있는 사용자 ID. 비어 있으면(미설정) 누구나 허용. */
    @Value("${reflection.allowed-user-id:}")
    private String allowedUserIdRaw;

    public ReflectionController(ReflectionService reflectionService,
                                EliciterService eliciterService,
                                ReflectionStorageService storageService) {
        this.reflectionService = reflectionService;
        this.eliciterService = eliciterService;
        this.storageService = storageService;
    }

    /**
     * 1단계: 묶기 + 개요. 동기 응답(빠름).
     * 프론트는 이 결과를 보여주고 멈춘 뒤, 사용자가 "독후감 만들기"를 누를 때 /compose로 그대로 돌려준다.
     */
    @PostMapping("/cluster")
    public ApiResponse<ClusterResult> cluster(@RequestParam Long bookId, HttpServletRequest request) {
        Long userId = resolveUserId(request);
        return ApiResponse.success(reflectionService.clusterOnly(userId, bookId));
    }

    /**
     * 2단계: 섹션 엮기. 프론트가 돌려준 묶기/개요 결과로 섹션을 SSE 스트리밍.
     */
    @PostMapping(value = "/compose", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter compose(@RequestBody ComposeRequest body, HttpServletRequest request) {
        Long userId = resolveUserId(request);
        SseEmitter emitter = new SseEmitter(5 * 60 * 1000L);
        emitter.onTimeout(emitter::complete);
        emitter.onError(e -> emitter.complete());
        reflectionService.composeSectionsAsync(userId, body, emitter);
        return emitter;
    }

    /**
     * 감상 더 끌어내기(Eliciter) — 한 턴씩 주고받는 일반 요청/응답.
     * 매 턴 전체 대화 내역을 받는 stateless 방식.
     */
    @PostMapping("/elicit")
    public ApiResponse<ElicitResponse> elicit(@RequestBody ElicitRequest body,
                                              HttpServletRequest request) {
        resolveUserId(request); // 인증 확인(대화 자체는 stateless라 userId를 따로 쓰진 않음)
        return ApiResponse.success(eliciterService.talk(body));
    }

    /**
     * 대화로 끌어낸 감상을 reading_record로 일괄 저장.
     * "정리하기"를 누를 때 프론트가 수집한 (질문, 감상) 쌍을 보낸다.
     * 저장된 감상은 다음 엮기 때 DB에서 재료로 읽힌다.
     */
    @PostMapping("/elicit/save")
    public ApiResponse<Integer> saveDrawn(@RequestBody ElicitSaveRequest body,
                                          HttpServletRequest request) {
        Long userId = resolveUserId(request);
        int saved = eliciterService.saveDrawn(userId, body);
        return ApiResponse.success(saved + "개의 감상을 기록에 더했습니다.", saved);
    }

    // ── 완성 독후감 저장/조회/수정/삭제 ──────────────────────────────

    /** 저장 또는 수정(upsert). 마크다운 한 덩어리. */
    @PostMapping("/save")
    public ApiResponse<ReflectionResponse> save(@RequestBody ReflectionSaveRequest body,
                                                HttpServletRequest request) {
        Long userId = resolveUserId(request);
        return ApiResponse.success(storageService.save(userId, body));
    }

    /** 저장된 독후감 조회. 없으면 success(null). */
    @GetMapping("/saved")
    public ApiResponse<ReflectionResponse> getSaved(@RequestParam Long bookId,
                                                    HttpServletRequest request) {
        Long userId = resolveUserId(request);
        return ApiResponse.success(storageService.get(userId, bookId).orElse(null));
    }

    /** 독후감 존재 여부(진입 분기용). */
    @GetMapping("/exists")
    public ApiResponse<Map<String, Boolean>> exists(@RequestParam Long bookId,
                                                    HttpServletRequest request) {
        Long userId = resolveUserId(request);
        boolean exists = storageService.exists(userId, bookId);
        return ApiResponse.success(Map.of("exists", exists));
    }

    /** 독후감 삭제(다시 생성하고 싶을 때). */
    @DeleteMapping("/saved")
    public ApiResponse<Void> deleteSaved(@RequestParam Long bookId,
                                         HttpServletRequest request) {
        Long userId = resolveUserId(request);
        storageService.delete(userId, bookId);
        return ApiResponse.success("삭제했습니다.");
    }

    private Long resolveUserId(HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("USER_ID");
        if (userId == null) throw new AuthException("인증이 필요합니다.");
        // 독후감 기능은 지정된 사용자만 사용 가능(설정 시). 프론트가 아닌 서버에서 차단.
        if (allowedUserIdRaw != null && !allowedUserIdRaw.isBlank()) {
            Long allowed = Long.valueOf(allowedUserIdRaw.trim());
            if (!allowed.equals(userId)) {
                throw new AuthException("이 기능을 사용할 권한이 없습니다.");
            }
        }
        return userId;
    }
}