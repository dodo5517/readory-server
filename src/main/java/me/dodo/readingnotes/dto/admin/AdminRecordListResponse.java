package me.dodo.readingnotes.dto.admin;

import me.dodo.readingnotes.domain.ReadingRecord;

import java.time.LocalDateTime;

public record AdminRecordListResponse(
        Long id,
        String username,
        String rawTitle,
        String rawAuthor,
        ReadingRecord.MatchStatus matchStatus,
        LocalDateTime createdAt,
        LocalDateTime recordedAt
) {
    public static AdminRecordListResponse from(ReadingRecord record) {
        return new AdminRecordListResponse(
                record.getId(),
                record.getUser().getUsername(),
                record.getRawTitle(),
                record.getRawAuthor(),
                record.getMatchStatus(),
                record.getCreatedAt(),
                record.getRecordedAt()
        );
    }
}
