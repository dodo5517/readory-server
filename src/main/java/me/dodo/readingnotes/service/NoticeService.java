package me.dodo.readingnotes.service;

import jakarta.persistence.EntityNotFoundException;
import me.dodo.readingnotes.domain.Notice;
import me.dodo.readingnotes.dto.notice.NoticeResponse;
import me.dodo.readingnotes.dto.notice.NoticeUpdateRequest;
import me.dodo.readingnotes.repository.NoticeRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class NoticeService {

    private final NoticeRepository noticeRepository;

    public NoticeService(NoticeRepository noticeRepository) {
        this.noticeRepository = noticeRepository;
    }

    // 활성화된 공지 조회 (없으면 null 반환)
    @Transactional(readOnly = true)
    public NoticeResponse getActiveNotice() {
        return noticeRepository.findTopByEnabledTrueOrderByUpdatedAtDesc()
                .map(NoticeResponse::from)
                .orElse(null);
    }

    // 관리자용: 활성 공지 조회, 없으면 가장 최근 것
    @Transactional(readOnly = true)
    public NoticeResponse getNoticeForAdmin() {
        return noticeRepository.findTopByEnabledTrueOrderByUpdatedAtDesc()
                .or(() -> noticeRepository.findAllByOrderByCreatedAtDesc().stream().findFirst())
                .map(NoticeResponse::from)
                .orElse(null);
    }

    // 관리자용: 전체 이력 조회 (최신순)
    @Transactional(readOnly = true)
    public List<NoticeResponse> getAllNotices() {
        return noticeRepository.findAllByOrderByCreatedAtDesc()
                .stream()
                .map(NoticeResponse::from)
                .collect(Collectors.toList());
    }

    // 관리자용: 기존 공지 수정 (enabled 토글 등)
    @Transactional
    public NoticeResponse updateNotice(Long id, NoticeUpdateRequest request) {
        Notice notice = noticeRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("공지를 찾을 수 없습니다. noticeId=" + id));
        if (request.getMessage() != null) notice.updateMessage(request.getMessage());
        if (request.getEnabled() != null) notice.changeEnabled(request.getEnabled());
        return NoticeResponse.from(noticeRepository.save(notice));
    }

    // 관리자용: 새 공지 insert (이전 공지 전부 비활성화)
    @Transactional
    public NoticeResponse createNotice(NoticeUpdateRequest request) {
        noticeRepository.findAll().forEach(n -> {
            if (n.isEnabled()) {
                n.changeEnabled(false);
                noticeRepository.save(n);
            }
        });

        Notice notice = Notice.create(
                request.getMessage() != null ? request.getMessage() : "",
                request.getEnabled() != null ? request.getEnabled() : true
        );
        return NoticeResponse.from(noticeRepository.save(notice));
    }
}