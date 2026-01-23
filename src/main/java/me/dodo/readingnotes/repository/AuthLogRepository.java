package me.dodo.readingnotes.repository;

import me.dodo.readingnotes.domain.UserAuthLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface AuthLogRepository extends JpaRepository<UserAuthLog, Long> {
    @Query("""
        select l
        from UserAuthLog l
        where
          (:lowKw is null
            or lower(l.identifier) like :lowKw
            or lower(l.ipAddress) like :lowKw
          )
          and (:type is null or l.eventType = :type)
          and (:result is null or l.result = :result)
        """)
    Page<UserAuthLog> searchLogs(
            @Param("lowKw") String lowKw,
            @Param("type") UserAuthLog.AuthEventType type,
            @Param("result") UserAuthLog.AuthResult result,
            Pageable pageable
    );

}
