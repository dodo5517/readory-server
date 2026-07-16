package me.dodo.readingnotes.controller;

import jakarta.servlet.http.HttpServletRequest;
import me.dodo.readingnotes.dto.common.ApiResponse;
import me.dodo.readingnotes.dto.reading.HighlightCreateRequest;
import me.dodo.readingnotes.dto.reading.HighlightItem;
import me.dodo.readingnotes.exception.AuthException;
import me.dodo.readingnotes.service.RecordHighlightService;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/records")
public class RecordHighlightController {

    private final RecordHighlightService service;

    public RecordHighlightController(RecordHighlightService service) {
        this.service = service;
    }

    // 하이라이트 추가
    @PostMapping("/{recordId}/highlights")
    public ApiResponse<HighlightItem> add(
            @PathVariable Long recordId,
            @RequestBody HighlightCreateRequest req,
            HttpServletRequest request) {
        Long userId = resolveUserId(request);
        return ApiResponse.success(service.add(userId, recordId, req));
    }

    // 하이라이트 삭제
    @DeleteMapping("/highlights/{highlightId}")
    public ApiResponse<Void> delete(
            @PathVariable Long highlightId,
            HttpServletRequest request) {
        Long userId = resolveUserId(request);
        service.delete(userId, highlightId);
        return ApiResponse.success("하이라이트가 삭제되었습니다.");
    }

    private Long resolveUserId(HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("USER_ID");
        if (userId == null) {
            throw new AuthException("인증이 필요합니다.");
        }
        return userId;
    }
}
