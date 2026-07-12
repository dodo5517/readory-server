package me.dodo.readingnotes.dto.notice;

import me.dodo.readingnotes.domain.Notice;

import java.time.LocalDateTime;

public record NoticeResponse(
        Long id,
        String message,
        boolean enabled,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
    public static NoticeResponse from(Notice n) {
        return new NoticeResponse(n.getId(), n.getMessage(), n.isEnabled(), n.getCreatedAt(), n.getUpdatedAt());
    }
}
