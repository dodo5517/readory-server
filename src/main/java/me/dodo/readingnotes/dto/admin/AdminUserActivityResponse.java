package me.dodo.readingnotes.dto.admin;

import java.time.LocalDateTime;

public class AdminUserActivityResponse {

    private final Long userId;
    private final String username;
    private final String userEmail;
    private final long totalRecords;
    private final LocalDateTime lastRecordedAt;

    public AdminUserActivityResponse(Long userId, String username, String userEmail,
                                     long totalRecords, LocalDateTime lastRecordedAt) {
        this.userId = userId;
        this.username = username;
        this.userEmail = userEmail;
        this.totalRecords = totalRecords;
        this.lastRecordedAt = lastRecordedAt;
    }

    public Long getUserId() {
        return userId;
    }
    public String getUsername() {
        return username;
    }
    public String getUserEmail() {
        return userEmail;
    }
    public long getTotalRecords() {
        return totalRecords;
    }
    public LocalDateTime getLastRecordedAt() {
        return lastRecordedAt;
    }
}