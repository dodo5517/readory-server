package me.dodo.readingnotes.dto.user;

import me.dodo.readingnotes.domain.User;

public record UserResponse(
        Long id,
        String username,
        String email,
        String profileImageUrl,
        String role,
        String maskedApiKey
) {
    public static UserResponse from(User user) {
        return new UserResponse(
                user.getId(),
                user.getUsername(),
                user.getEmail(),
                user.getProfileImageUrl(),
                user.getRole(),
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
