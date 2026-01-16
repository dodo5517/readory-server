package me.dodo.readingnotes.repository;

import me.dodo.readingnotes.domain.ApiLog;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ApiLogRepository extends JpaRepository<ApiLog, Long> {
}
