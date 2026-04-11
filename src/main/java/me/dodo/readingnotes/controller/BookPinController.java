package me.dodo.readingnotes.controller;

import jakarta.servlet.http.HttpServletRequest;
import me.dodo.readingnotes.service.BookPinService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/books/{bookId}/pin")
public class BookPinController {

    private final BookPinService bookPinService;

    public BookPinController(BookPinService bookPinService) {
        this.bookPinService = bookPinService;
    }

    @PostMapping
    public ResponseEntity<Void> pin(@PathVariable Long bookId, HttpServletRequest request) {
        bookPinService.pin(resolveUserId(request), bookId);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping
    public ResponseEntity<Void> unpin(@PathVariable Long bookId, HttpServletRequest request) {
        bookPinService.unpin(resolveUserId(request), bookId);
        return ResponseEntity.noContent().build();
    }

    private Long resolveUserId(HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("USER_ID");
        if (userId == null) throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "인증이 필요합니다.");
        return userId;
    }
}