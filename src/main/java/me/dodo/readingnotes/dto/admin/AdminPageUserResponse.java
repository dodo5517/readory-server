package me.dodo.readingnotes.dto.admin;

import me.dodo.readingnotes.domain.User;

public record AdminPageUserResponse(
        Long id,
        String username,
        String email,
        String profileImageUrl,
        String role,
        User.UserStatus status,
        String provider,
        String maskedApiKey
) {
    public static AdminPageUserResponse from(User user) {
        return new AdminPageUserResponse(
                user.getId(),
                user.getUsername(),
                user.getEmail(),
                user.getProfileImageUrl(),
                user.getRole(),
                user.getUserStatus(),
                user.getProvider(),
                maskApiKey(user.getApiKey())
        );
    }

    // api_key 마스킹 하기
    private static String maskApiKey(String apiKey) {
        if (apiKey == null || apiKey.length() < 4) return "****";
        int visibleCount = 4;
        int maskCount = apiKey.length() - visibleCount;
        return "*".repeat(maskCount) + apiKey.substring(maskCount);
    }
}
