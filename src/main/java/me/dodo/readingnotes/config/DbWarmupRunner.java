package me.dodo.readingnotes.config;

import me.dodo.readingnotes.repository.NoticeRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
public class DbWarmupRunner {

    private static final Logger log = LoggerFactory.getLogger(DbWarmupRunner.class);

    private final NoticeRepository noticeRepository;

    public DbWarmupRunner(NoticeRepository noticeRepository) {
        this.noticeRepository = noticeRepository;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void warmUp() {
        long start = System.currentTimeMillis();
        try {
            long count = noticeRepository.count();
            log.info("DB 웜업 완료: {}ms (notice count={})", System.currentTimeMillis() - start, count);
        } catch (Exception e) {
            log.warn("DB 웜업 실패: {}ms, {}", System.currentTimeMillis() - start, e.toString());
        }
    }
}
