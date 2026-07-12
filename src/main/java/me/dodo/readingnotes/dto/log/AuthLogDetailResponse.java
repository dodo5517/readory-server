package me.dodo.readingnotes.dto.log;

import me.dodo.readingnotes.domain.UserAuthLog;

public record AuthLogDetailResponse(
        Long id,
        Long userId,
        UserAuthLog.AuthEventType eventType,
        UserAuthLog.AuthResult result,
        String failResponse,
        String ipAddress,
        String userAgent,
        String identifier,
        String createdAt
) {
    public static AuthLogDetailResponse from(UserAuthLog userAuthLog) {
        return new AuthLogDetailResponse(
                userAuthLog.getId(),
                userAuthLog.getUser() != null ? userAuthLog.getUser().getId() : null,
                userAuthLog.getEventType(),
                userAuthLog.getResult(),
                userAuthLog.getFailReason(),
                userAuthLog.getIpAddress(),
                userAuthLog.getUserAgent(),
                userAuthLog.getIdentifier(),
                userAuthLog.getCreatedAt().toString()
        );
    }
}
