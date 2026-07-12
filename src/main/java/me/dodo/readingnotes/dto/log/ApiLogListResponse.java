package me.dodo.readingnotes.dto.log;

import me.dodo.readingnotes.domain.ApiLog;

import java.time.LocalDateTime;

public record ApiLogListResponse(
        Long id,
        LocalDateTime createdAt,

        String method,
        String path,

        int statusCode,
        ApiLog.Result result,

        Integer executionTimeMs,

        Long userId,
        String userRole
) {
    public static ApiLogListResponse from(ApiLog log) {
        return new ApiLogListResponse(
                log.getId(),
                log.getCreatedAt(),
                log.getMethod(),
                log.getPath(),
                log.getStatusCode(),
                log.getResult(),
                log.getExecutionTimeMs(),
                log.getUser() == null ? null : log.getUser().getId(),
                log.getUserRole()
        );
    }
}
