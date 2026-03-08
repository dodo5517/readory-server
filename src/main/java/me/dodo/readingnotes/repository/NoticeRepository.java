package me.dodo.readingnotes.repository;

import me.dodo.readingnotes.domain.Notice;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface NoticeRepository extends JpaRepository<Notice, Long> {
    // enabled=true인 공지 중 가장 최근 것
    Optional<Notice> findTopByEnabledTrueOrderByUpdatedAtDesc();

    // 전체 이력 (최신순)
    List<Notice> findAllByOrderByCreatedAtDesc();
}