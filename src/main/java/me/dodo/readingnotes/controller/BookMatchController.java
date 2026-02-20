package me.dodo.readingnotes.controller;


import me.dodo.readingnotes.dto.book.BookCandidate;
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

    // Book Table에서 후보를 조회함.
    @GetMapping("/local")
    public List<BookCandidate> candidatesLocal(@RequestParam("rawTitle") String rawTitle,
                                          @RequestParam(value = "rawAuthor", required = false) String rawAuthor,
                                          @RequestParam(value = "limit", defaultValue = "10") int limit) {
        return candidateService.findCandidatesLocal(rawTitle, rawAuthor, limit);
    }
    
    // 외부 api로 후보를 조회함
    @GetMapping("/external")
    public List<BookCandidate> candidatesExternal(@RequestParam("rawTitle") String rawTitle,
                                          @RequestParam(value = "rawAuthor", required = false) String rawAuthor,
                                          @RequestParam(value = "limit", defaultValue = "10") int limit) {
        return candidateService.findCandidatesExternal(rawTitle, rawAuthor, limit);
    }
}
