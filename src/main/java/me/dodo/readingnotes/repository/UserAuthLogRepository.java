package me.dodo.readingnotes.repository;

import me.dodo.readingnotes.domain.UserAuthLog;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserAuthLogRepository extends JpaRepository<UserAuthLog, Long> {
}
