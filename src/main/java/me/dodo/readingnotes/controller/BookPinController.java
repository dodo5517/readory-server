package me.dodo.readingnotes.controller;

import jakarta.servlet.http.HttpServletRequest;
import me.dodo.readingnotes.dto.common.ApiResponse;
import me.dodo.readingnotes.exception.AuthException;
import me.dodo.readingnotes.service.BookPinService;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/books/{bookId}/pin")
public class BookPinController {

    private final BookPinService bookPinService;

    public BookPinController(BookPinService bookPinService) {
        this.bookPinService = bookPinService;
    }

    @PostMapping
    public ApiResponse<Void> pin(@PathVariable Long bookId, HttpServletRequest request) {
        bookPinService.pin(resolveUserId(request), bookId);
        return ApiResponse.success("고정되었습니다.");
    }

    @DeleteMapping
    public ApiResponse<Void> unpin(@PathVariable Long bookId, HttpServletRequest request) {
        bookPinService.unpin(resolveUserId(request), bookId);
        return ApiResponse.success("고정이 해제되었습니다.");
    }

    private Long resolveUserId(HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("USER_ID");
        if (userId == null) throw new AuthException("인증이 필요합니다.");
        return userId;
    }
}
