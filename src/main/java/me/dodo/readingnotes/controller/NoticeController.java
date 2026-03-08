package me.dodo.readingnotes.controller;

import me.dodo.readingnotes.dto.notice.NoticeResponse;
import me.dodo.readingnotes.service.NoticeService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/notice")
public class NoticeController {

    private final NoticeService noticeService;

    public NoticeController(NoticeService noticeService) {
        this.noticeService = noticeService;
    }

    // 인증 불필요 - 로그인 화면에서도 호출
    @GetMapping
    public ResponseEntity<NoticeResponse> getActiveNotice() {
        NoticeResponse notice = noticeService.getActiveNotice();
        if (notice == null) return ResponseEntity.noContent().build();
        return ResponseEntity.ok(notice);
    }
}