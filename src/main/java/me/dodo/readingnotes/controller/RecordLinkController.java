package me.dodo.readingnotes.controller;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import me.dodo.readingnotes.dto.book.LinkBookRequest;
import me.dodo.readingnotes.service.BookLinkService;
import me.dodo.readingnotes.util.JwtTokenProvider;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/records")
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
    public Boolean link(@PathVariable("id") Long id,
                        @RequestBody @Valid LinkBookRequest req,
                        HttpServletRequest httpRequest) {

        // JwtAuthFilter가 심어준 값 사용
        Long userId = (Long) httpRequest.getAttribute("USER_ID");
        if (userId == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "인증이 필요합니다.");
        }

        // 본인 기록인지 확인
        bookLinkService.assertSelf(id, userId);
        // 책 매칭
        bookLinkService.linkRecord(id, req);
        return true;
    }

    // 매칭 정보 삭제
    @PostMapping("/{id}/remove")
    public Boolean remove(@PathVariable("id") Long id,
                          HttpServletRequest httpRequest) {
        // JwtAuthFilter가 심어준 값 사용
        Long userId = (Long) httpRequest.getAttribute("USER_ID");
        if (userId == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "인증이 필요합니다.");
        }

        // 본인 기록인지 확인
        bookLinkService.assertSelf(id, userId);

        // 매칭 정보 삭제
        bookLinkService.removeBookMatch(id);
        return true;
    }
}
