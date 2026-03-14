package me.dodo.readingnotes.controller;

import jakarta.servlet.http.HttpServletRequest;
import me.dodo.readingnotes.dto.book.BookCommentRequest;
import me.dodo.readingnotes.dto.book.BookCommentResponse;
import me.dodo.readingnotes.service.BookCommentService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;

@RestController
@RequestMapping("/api/records/books/{bookId}/comment")
public class BookCommentController {

    private final BookCommentService bookCommentService;

    public BookCommentController(BookCommentService bookCommentService) {
        this.bookCommentService = bookCommentService;
    }

    // 책 코멘트 조회
    @GetMapping
    public ResponseEntity<BookCommentResponse> getComment(
            @PathVariable Long bookId,
            HttpServletRequest request) {
        Long userId = resolveUserId(request);
        BookCommentResponse response = bookCommentService.getComment(userId, bookId);
        if (response == null) {
            return ResponseEntity.noContent().build(); // 204: 코멘트 없음
        }
        return ResponseEntity.ok(response);
    }

    // 책 코멘트 저장/수정 (upsert)
    @PutMapping
    public ResponseEntity<BookCommentResponse> upsertComment(
            @PathVariable Long bookId,
            @RequestBody BookCommentRequest req,
            HttpServletRequest request) {
        Long userId = resolveUserId(request);
        return ResponseEntity.ok(bookCommentService.upsertComment(userId, bookId, req));
    }

    // 책 코멘트 삭제
    @DeleteMapping
    public ResponseEntity<Void> deleteComment(
            @PathVariable Long bookId,
            HttpServletRequest request) {
        Long userId = resolveUserId(request);
        bookCommentService.deleteComment(userId, bookId);
        return ResponseEntity.noContent().build();
    }

    // userId 추출
    private Long resolveUserId(HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("USER_ID");
        if (userId == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "인증이 필요합니다.");
        }
        return userId;
    }
}