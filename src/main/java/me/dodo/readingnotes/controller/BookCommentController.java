package me.dodo.readingnotes.controller;

import jakarta.servlet.http.HttpServletRequest;
import me.dodo.readingnotes.dto.book.BookCommentRequest;
import me.dodo.readingnotes.dto.book.BookCommentResponse;
import me.dodo.readingnotes.dto.common.ApiResponse;
import me.dodo.readingnotes.exception.AuthException;
import me.dodo.readingnotes.service.BookCommentService;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/records/books/{bookId}/comment")
public class BookCommentController {

    private final BookCommentService bookCommentService;

    public BookCommentController(BookCommentService bookCommentService) {
        this.bookCommentService = bookCommentService;
    }

    // 책 코멘트 조회
    @GetMapping
    public ApiResponse<BookCommentResponse> getComment(
            @PathVariable Long bookId,
            HttpServletRequest request) {
        Long userId = resolveUserId(request);
        return ApiResponse.success(bookCommentService.getComment(userId, bookId));
    }

    // 책 코멘트 저장/수정 (upsert)
    @PutMapping
    public ApiResponse<BookCommentResponse> upsertComment(
            @PathVariable Long bookId,
            @RequestBody BookCommentRequest req,
            HttpServletRequest request) {
        Long userId = resolveUserId(request);
        return ApiResponse.success(bookCommentService.upsertComment(userId, bookId, req));
    }

    // 책 코멘트 삭제
    @DeleteMapping
    public ApiResponse<Void> deleteComment(
            @PathVariable Long bookId,
            HttpServletRequest request) {
        Long userId = resolveUserId(request);
        bookCommentService.deleteComment(userId, bookId);
        return ApiResponse.success("코멘트가 삭제되었습니다.");
    }

    private Long resolveUserId(HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("USER_ID");
        if (userId == null) {
            throw new AuthException("인증이 필요합니다.");
        }
        return userId;
    }
}
