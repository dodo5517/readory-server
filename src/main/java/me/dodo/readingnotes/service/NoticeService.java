package me.dodo.readingnotes.service;

import me.dodo.readingnotes.domain.Notice;
import me.dodo.readingnotes.dto.notice.NoticeResponse;
import me.dodo.readingnotes.dto.notice.NoticeUpdateRequest;
import me.dodo.readingnotes.repository.NoticeRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class NoticeService {

    private final NoticeRepository noticeRepository;

    public NoticeService(NoticeRepository noticeRepository) {
        this.noticeRepository = noticeRepository;
    }

    private NoticeResponse toResponse(Notice n) {
        return new NoticeResponse(n.getId(), n.getMessage(), n.isEnabled(), n.getCreatedAt(), n.getUpdatedAt());
    }

    // 활성화된 공지 조회 (없으면 null 반환)
    @Transactional(readOnly = true)
    public NoticeResponse getActiveNotice() {
        return noticeRepository.findTopByEnabledTrueOrderByUpdatedAtDesc()
                .map(this::toResponse)
                .orElse(null);
    }

    // 관리자용: 활성 공지 조회, 없으면 가장 최근 것
    @Transactional(readOnly = true)
    public NoticeResponse getNoticeForAdmin() {
        return noticeRepository.findTopByEnabledTrueOrderByUpdatedAtDesc()
                .or(() -> noticeRepository.findAllByOrderByCreatedAtDesc().stream().findFirst())
                .map(this::toResponse)
                .orElse(null);
    }

    // 관리자용: 전체 이력 조회 (최신순)
    @Transactional(readOnly = true)
    public List<NoticeResponse> getAllNotices() {
        return noticeRepository.findAllByOrderByCreatedAtDesc()
                .stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    // 관리자용: 기존 공지 수정 (enabled 토글 등)
    @Transactional
    public NoticeResponse updateNotice(Long id, NoticeUpdateRequest request) {
        Notice notice = noticeRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("공지를 찾을 수 없습니다."));
        if (request.getMessage() != null) notice.setMessage(request.getMessage());
        if (request.getEnabled() != null) notice.setEnabled(request.getEnabled());
        notice.setUpdatedAt(LocalDateTime.now());
        return toResponse(noticeRepository.save(notice));
    }

    // 관리자용: 새 공지 insert (이전 공지 전부 비활성화)
    @Transactional
    public NoticeResponse createNotice(NoticeUpdateRequest request) {
        noticeRepository.findAll().forEach(n -> {
            if (n.isEnabled()) {
                n.setEnabled(false);
                n.setUpdatedAt(LocalDateTime.now());
                noticeRepository.save(n);
            }
        });

        Notice notice = new Notice();
        notice.setMessage(request.getMessage() != null ? request.getMessage() : "");
        notice.setEnabled(request.getEnabled() != null ? request.getEnabled() : true);
        notice.setCreatedAt(LocalDateTime.now());
        notice.setUpdatedAt(LocalDateTime.now());
        return toResponse(noticeRepository.save(notice));
    }
}