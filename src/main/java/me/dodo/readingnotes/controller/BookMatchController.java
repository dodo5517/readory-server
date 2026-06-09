package me.dodo.readingnotes.controller;


import me.dodo.readingnotes.dto.book.BookCandidate;
import me.dodo.readingnotes.dto.common.ApiResponse;
import me.dodo.readingnotes.service.BookCandidateService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/books/candidates")
public class BookMatchController {

    private final BookCandidateService candidateService;

    public BookMatchController(BookCandidateService candidateService) {
        this.candidateService = candidateService;
    }

    @GetMapping("/local")
    public ApiResponse<List<BookCandidate>> candidatesLocal(
            @RequestParam("rawTitle") String rawTitle,
            @RequestParam(value = "rawAuthor", required = false) String rawAuthor,
            @RequestParam(value = "limit", defaultValue = "10") int limit) {
        return ApiResponse.success(candidateService.findCandidatesLocal(rawTitle, rawAuthor, limit));
    }

    @GetMapping("/external")
    public ApiResponse<List<BookCandidate>> candidatesExternal(
            @RequestParam("rawTitle") String rawTitle,
            @RequestParam(value = "rawAuthor", required = false) String rawAuthor,
            @RequestParam(value = "limit", defaultValue = "10") int limit) {
        return ApiResponse.success(candidateService.findCandidatesExternal(rawTitle, rawAuthor, limit));
    }
}
