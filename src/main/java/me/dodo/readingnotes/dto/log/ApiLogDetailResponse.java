package me.dodo.readingnotes.dto.log;

import me.dodo.readingnotes.domain.ApiLog;

import java.time.LocalDateTime;

public record ApiLogDetailResponse(
        Long id,
        Long userId,
        String userRole,

        String method,
        String path,
        String queryString,

        int statusCode,
        ApiLog.Result result,

        String ipAddress,
        String userAgent,

        int executionTimeMs,

        String errorCode,
        String errorMessage,

        LocalDateTime createdAt
) {
    public static ApiLogDetailResponse from(ApiLog log) {
        return new ApiLogDetailResponse(
                log.getId(),
                log.getUser() == null ? null : log.getUser().getId(),
                log.getUserRole(),
                log.getMethod(),
                log.getPath(),
                log.getQueryString(),
                log.getStatusCode(),
                log.getResult(),
                log.getIpAddress(),
                log.getUserAgent(),
                log.getExecutionTimeMs(),
                log.getErrorCode(),
                log.getErrorMessage(),
                log.getCreatedAt()
        );
    }
}
