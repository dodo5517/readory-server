package me.dodo.readingnotes.dto.admin;

import me.dodo.readingnotes.domain.User;

public class AdminPageUserResponse {
    private Long id;
    private String username;
    private String email;
    private String profileImageUrl;
    private String role;
    private User.UserStatus status;
    private String provider;
    private String maskedApiKey;

    // UserResponse 객체
    public AdminPageUserResponse(User user) {
        this.id = user.getId();
        this.username = user.getUsername();
        this.email = user.getEmail();
        this.profileImageUrl = user.getProfileImageUrl();
        this.role = user.getRole();
        this.status = user.getUserStatus();
        this.provider = user.getProvider();

        String apiKey = user.getApiKey();
        this.maskedApiKey = maskApiKey(apiKey);
    }

    // api_key 마스킹 하기
    private String maskApiKey(String apiKey) {
        if (apiKey == null || apiKey.length() < 4) return "****";
        int visibleCount = 4;
        int maskCount = apiKey.length() - visibleCount;
        return "*".repeat(maskCount) + apiKey.substring(maskCount);
    }

    // Getter
    public Long getId() { return id; }
    public String getUsername() { return username; }
    public String getEmail() { return email; }
    public String getProfileImageUrl() { return profileImageUrl; }
    public String getRole() { return role; }
    public User.UserStatus getStatus() { return status; }
    public String getProvider() { return provider; }
    public String getMaskedApiKey() { return maskedApiKey; }
}
