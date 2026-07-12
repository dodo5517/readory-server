package me.dodo.readingnotes.dto.admin;

import me.dodo.readingnotes.domain.ReadingRecord;

import java.time.LocalDateTime;

public record AdminRecordDetailResponse(
        Long id,

        // 유저 정보
        Long userId,
        String username,
        String userEmail,

        // 책 정보 (매칭된 경우)
        Long bookId,
        String bookTitle,
        String bookAuthor,
        String bookCoverUrl,

        // 원본 입력값
        String rawTitle,
        String rawAuthor,

        // 상태 및 시간
        ReadingRecord.MatchStatus matchStatus,
        LocalDateTime createdAt,
        LocalDateTime recordedAt,
        LocalDateTime updatedAt,
        LocalDateTime matchedAt
) {
    public static AdminRecordDetailResponse from(ReadingRecord record) {
        Long bookId = null;
        String bookTitle = null;
        String bookAuthor = null;
        String bookCoverUrl = null;
        if (record.getBook() != null) {
            bookId = record.getBook().getId();
            bookTitle = record.getBook().getTitle();
            bookAuthor = record.getBook().getAuthor();
            bookCoverUrl = record.getBook().getCoverUrl();
        }

        return new AdminRecordDetailResponse(
                record.getId(),
                record.getUser().getId(),
                record.getUser().getUsername(),
                record.getUser().getEmail(),
                bookId,
                bookTitle,
                bookAuthor,
                bookCoverUrl,
                record.getRawTitle(),
                record.getRawAuthor(),
                record.getMatchStatus(),
                record.getCreatedAt(),
                record.getRecordedAt(),
                record.getUpdatedAt(),
                record.getMatchedAt()
        );
    }
}
