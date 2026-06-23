package me.dodo.readingnotes.controller;

import jakarta.servlet.http.HttpServletRequest;
import me.dodo.readingnotes.dto.common.ApiResponse;
import me.dodo.readingnotes.dto.reflection.ClusterResult;
import me.dodo.readingnotes.dto.reflection.ComposeRequest;
import me.dodo.readingnotes.dto.reflection.ElicitRequest;
import me.dodo.readingnotes.dto.reflection.ElicitResponse;
import me.dodo.readingnotes.dto.reflection.ElicitSaveRequest;
import me.dodo.readingnotes.exception.AuthException;
import me.dodo.readingnotes.service.reflection.EliciterService;
import me.dodo.readingnotes.service.reflection.ReflectionService;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@RestController
@RequestMapping("/api/reflection")
public class ReflectionController {

    private final ReflectionService reflectionService;
    private final EliciterService eliciterService;

    public ReflectionController(ReflectionService reflectionService,
                                EliciterService eliciterService) {
        this.reflectionService = reflectionService;
        this.eliciterService = eliciterService;
    }

    /**
     * 1단계: 묶기 + 개요. 동기 응답.
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

    private Long resolveUserId(HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("USER_ID");
        if (userId == null) throw new AuthException("인증이 필요합니다.");
        return userId;
    }
}