package me.dodo.readingnotes.repository;

import me.dodo.readingnotes.domain.Notice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface NoticeRepository extends JpaRepository<Notice, Long> {
    // enabled=true인 공지 중 가장 최근 것
    Optional<Notice> findTopByEnabledTrueOrderByUpdatedAtDesc();

    // 전체 이력 (최신순)
    List<Notice> findAllByOrderByCreatedAtDesc();

    @Modifying
    @Query("UPDATE Notice n SET n.enabled = false, n.updatedAt = :now WHERE n.enabled = true AND n.createdAt < :threshold")
    int expireNoticesBefore(@Param("threshold") LocalDateTime threshold, @Param("now") LocalDateTime now);
}