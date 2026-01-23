package me.dodo.readingnotes.repository;

import me.dodo.readingnotes.domain.ApiLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ApiLogRepository extends JpaRepository<ApiLog, Long> {
    @Query("""
        select a
        from ApiLog a
        where (:keyword is null
              or lower(a.path) like :keyword
              or lower(a.queryString) like :keyword
              or lower(a.errorMessage) like :keyword
              or lower(a.ipAddress) like :keyword
              or lower(a.userAgent) like :keyword
        )
        and (:result is null or a.result = :result)
        and (:statusCode is null or a.statusCode = :statusCode)
        and (:method is null or a.method = :method)
        """)
    Page<ApiLog> searchLogs(
            @Param("keyword") String keyword,
            @Param("result") ApiLog.Result result,
            @Param("statusCode") Integer statusCode,
            @Param("method") String method,
            Pageable pageable
    );
}
