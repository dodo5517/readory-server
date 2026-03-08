package me.dodo.readingnotes.scheduler;

import me.dodo.readingnotes.repository.NoticeRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Component
public class NoticeScheduler {

    private static final Logger log = LoggerFactory.getLogger(NoticeScheduler.class);

    private final NoticeRepository noticeRepository;

    public NoticeScheduler(NoticeRepository noticeRepository) {
        this.noticeRepository = noticeRepository;
    }

    // 매일 오전 3시 실행
    @Scheduled(cron = "0 0 3 * * *")
    @Transactional
    public void expireOldNotices() {
        LocalDateTime threshold = LocalDateTime.now().minusDays(30);

        noticeRepository.findAll().stream()
                .filter(n -> n.isEnabled() && n.getCreatedAt().isBefore(threshold))
                .forEach(n -> {
                    n.setEnabled(false);
                    n.setUpdatedAt(LocalDateTime.now());
                    noticeRepository.save(n);
                    log.info("공지 자동 만료: id={}, createdAt={}", n.getId(), n.getCreatedAt());
                });
    }
}