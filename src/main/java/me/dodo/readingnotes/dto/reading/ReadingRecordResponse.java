package me.dodo.readingnotes.dto.reading;

import me.dodo.readingnotes.domain.Book;
import me.dodo.readingnotes.domain.ReadingRecord;

import java.time.LocalDateTime;

public class ReadingRecordResponse {
    private Long id;
    private String title;
    private String author;
    private String sentence;
    private String comment;
    private Boolean matched;
    private Long bookId;
    private String coverUrl;
    private LocalDateTime createdAt;

    public ReadingRecordResponse(ReadingRecord r) {
        this.id = r.getId();
        this.createdAt = r.getCreatedAt();
        
        // 책 매칭된 상태인지 확인
        boolean isResolved = r.getMatchStatus() != null &&
                (r.getMatchStatus() == ReadingRecord.MatchStatus.RESOLVED_AUTO
                || r. getMatchStatus() == ReadingRecord.MatchStatus.RESOLVED_MANUAL);

        Book book = r.getBook();
        // 책이 매칭 완료된 상태라면 연결된 책 정보 사용.
        if (isResolved && book != null) {
            this.title = book.getTitle();
            this.author = book.getAuthor();
            this.sentence = r.getSentence();
            this.comment = r.getComment();
            this.matched = true;
            this.bookId = book.getId();
            this.coverUrl = book.getCoverUrl();
            this.createdAt = r.getCreatedAt();
        } else {
            // 매칭되지 않은 상태라면 raw 사용
            this.title = r.getRawTitle();
            this.author = r.getRawAuthor();
            this.sentence = r.getSentence();
            this.comment = r.getComment();
            this.matched = false;
            this.bookId = null;
            this.coverUrl = null;
            this.createdAt = r.getCreatedAt();
        }
    }

    // fromEntity를 위한 DTO 생성자
    public ReadingRecordResponse(Long id,
                                 String title,
                                 String author,
                                 String sentence,
                                 String comment,
                                 Boolean matched,
                                 Long bookId,
                                 String coverUrl,
                                 LocalDateTime createdAt) {
        this.id = id;
        this.title = title;
        this.author = author;
        this.sentence = sentence;
        this.comment = comment;
        this.matched = matched;
        this.bookId = bookId;
        this.coverUrl = coverUrl;
        this.createdAt = createdAt;
    }

    public static ReadingRecordResponse fromEntity(ReadingRecord record) {
        return new ReadingRecordResponse(
                record.getId(),
                record.getBook() != null ? record.getBook().getTitle() : record.getRawTitle(),
                record.getBook() != null ? record.getBook().getAuthor() : record.getRawAuthor(),
                record.getSentence(),
                record.getComment(),
                record.getBook() != null, // 매칭 여부
                record.getBook() != null ? record.getBook().getId() : null,
                record.getBook() != null ? record.getBook().getCoverUrl() : null,
                record.getCreatedAt()
        );
    }

    public Long getId() { return id; }
    public String getTitle() { return title; }
    public String getAuthor() { return author; }
    public String getSentence() { return sentence; }
    public String getComment() { return comment; }
    public Boolean getMatched() { return matched; }
    public Long getBookId() { return bookId; }
    public String getCoverUrl() { return coverUrl; }
    public LocalDateTime getCreatedAt() { return createdAt; }
}
