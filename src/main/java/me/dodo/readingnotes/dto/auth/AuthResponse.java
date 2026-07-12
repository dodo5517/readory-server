package me.dodo.readingnotes.dto.auth;

import me.dodo.readingnotes.dto.user.UserResponse;

public record AuthResponse(
        String message,
        UserResponse user,
        String accessToken,
        String refreshToken,
        Long expiresIn,
        Long serverTime
) {}
