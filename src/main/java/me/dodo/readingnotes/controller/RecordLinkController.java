package me.dodo.readingnotes.controller;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import me.dodo.readingnotes.dto.book.LinkBookRequest;
import me.dodo.readingnotes.service.BookLinkService;
import me.dodo.readingnotes.util.JwtTokenProvider;
import org.springframework.web.bind.annotation.*;

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

        // 토큰에서 userId 추출
        String accessToken = jwtTokenProvider.extractToken(httpRequest);
        jwtTokenProvider.assertValid(accessToken);
        Long userId = jwtTokenProvider.getUserIdFromToken(accessToken);
        
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
        // 토큰에서 userId 추출
        String accessToken = jwtTokenProvider.extractToken(httpRequest);
        jwtTokenProvider.assertValid(accessToken);
        Long userId = jwtTokenProvider.getUserIdFromToken(accessToken);

        // 본인 기록인지 확인
        bookLinkService.assertSelf(id, userId);

        // 매칭 정보 삭제
        bookLinkService.removeBookMatch(id);
        return true;
    }
}
