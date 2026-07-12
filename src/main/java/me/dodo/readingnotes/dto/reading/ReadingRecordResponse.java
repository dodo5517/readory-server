package me.dodo.readingnotes.dto.reading;

import me.dodo.readingnotes.domain.Book;
import me.dodo.readingnotes.domain.ReadingRecord;

import java.time.LocalDateTime;

public record ReadingRecordResponse(
        Long id,
        String title,
        String author,
        String sentence,
        String comment,
        Boolean matched,
        Long bookId,
        String coverUrl,
        LocalDateTime recordedAt
) {
    public static ReadingRecordResponse from(ReadingRecord r) {
        // 책 매칭된 상태인지 확인
        boolean isResolved = r.getMatchStatus() != null &&
                (r.getMatchStatus() == ReadingRecord.MatchStatus.RESOLVED_AUTO
                || r.getMatchStatus() == ReadingRecord.MatchStatus.RESOLVED_MANUAL);

        Book book = r.getBook();
        // 책이 매칭 완료된 상태라면 연결된 책 정보 사용.
        if (isResolved && book != null) {
            return new ReadingRecordResponse(
                    r.getId(),
                    book.getTitle(),
                    book.getAuthor(),
                    r.getSentence(),
                    r.getComment(),
                    true,
                    book.getId(),
                    book.getCoverUrl(),
                    r.getRecordedAt()
            );
        } else {
            // 매칭되지 않은 상태라면 raw 사용
            return new ReadingRecordResponse(
                    r.getId(),
                    r.getRawTitle(),
                    r.getRawAuthor(),
                    r.getSentence(),
                    r.getComment(),
                    false,
                    null,
                    null,
                    r.getRecordedAt()
            );
        }
    }
}
