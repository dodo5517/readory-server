package me.dodo.readingnotes.controller;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import me.dodo.readingnotes.dto.book.LinkBookRequest;
import me.dodo.readingnotes.dto.common.ApiResponse;
import me.dodo.readingnotes.exception.AuthException;
import me.dodo.readingnotes.service.BookLinkService;
import me.dodo.readingnotes.util.JwtTokenProvider;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/records")
public class RecordLinkController {

    private final BookLinkService bookLinkService;
    private final JwtTokenProvider jwtTokenProvider;

    public RecordLinkController(BookLinkService bookLinkService,
                                JwtTokenProvider jwtTokenProvider) {
        this.bookLinkService = bookLinkService;
        this.jwtTokenProvider = jwtTokenProvider;
    }

    // 매칭 확정
    @PostMapping("/{id}/link")
    public ApiResponse<Boolean> link(@PathVariable("id") Long id,
                                     @RequestBody @Valid LinkBookRequest req,
                                     HttpServletRequest httpRequest) {
        Long userId = (Long) httpRequest.getAttribute("USER_ID");
        if (userId == null) {
            throw new AuthException("인증이 필요합니다.");
        }

        bookLinkService.assertSelf(id, userId);
        bookLinkService.linkRecord(id, req);
        return ApiResponse.success(true);
    }

    // 매칭 정보 삭제
    @PostMapping("/{id}/remove")
    public ApiResponse<Boolean> remove(@PathVariable("id") Long id,
                                       HttpServletRequest httpRequest) {
        Long userId = (Long) httpRequest.getAttribute("USER_ID");
        if (userId == null) {
            throw new AuthException("인증이 필요합니다.");
        }

        bookLinkService.assertSelf(id, userId);
        bookLinkService.removeBookMatch(id);
        return ApiResponse.success(true);
    }
}
