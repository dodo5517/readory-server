package me.dodo.readingnotes.dto.admin;

import java.time.LocalDateTime;

public record AdminUserActivityResponse(
        Long userId,
        String username,
        String userEmail,
        long totalRecords,
        LocalDateTime lastRecordedAt
) {}
