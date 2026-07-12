package me.dodo.readingnotes.dto.log;

import me.dodo.readingnotes.domain.UserAuthLog;

public record AuthLogListResponse(
        Long id,
        Long userId,
        UserAuthLog.AuthEventType eventType,
        UserAuthLog.AuthResult result,
        String ipAddress,
        String createdAt
) {
    public static AuthLogListResponse from(UserAuthLog userAuthLog) {
        return new AuthLogListResponse(
                userAuthLog.getId(),
                userAuthLog.getUser() != null ? userAuthLog.getUser().getId() : null,
                userAuthLog.getEventType(),
                userAuthLog.getResult(),
                userAuthLog.getIpAddress(),
                userAuthLog.getCreatedAt().toString()
        );
    }
}
