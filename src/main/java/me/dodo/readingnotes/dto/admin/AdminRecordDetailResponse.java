package me.dodo.readingnotes.dto.admin;

import me.dodo.readingnotes.domain.ReadingRecord;
import java.time.LocalDateTime;

public class AdminRecordDetailResponse {
    private Long id;

    // 유저 정보
    private Long userId;
    private String username;
    private String userEmail;

    // 책 정보 (매칭된 경우)
    private Long bookId;
    private String bookTitle;
    private String bookAuthor;
    private String bookCoverUrl;

    // 원본 입력값
    private String rawTitle;
    private String rawAuthor;

    // 기록 내용
    private String sentence;
    private String comment;

    // 상태 및 시간
    private ReadingRecord.MatchStatus matchStatus;
    private LocalDateTime recordedAt;
    private LocalDateTime updatedAt;
    private LocalDateTime matchedAt;

    public AdminRecordDetailResponse(ReadingRecord record) {
        this.id = record.getId();

        // 유저 정보
        this.userId = record.getUser().getId();
        this.username = record.getUser().getUsername();
        this.userEmail = record.getUser().getEmail();

        // 책 정보
        if (record.getBook() != null) {
            this.bookId = record.getBook().getId();
            this.bookTitle = record.getBook().getTitle();
            this.bookAuthor = record.getBook().getAuthor();
            this.bookCoverUrl = record.getBook().getCoverUrl();
        }

        // 원본 입력값
        this.rawTitle = record.getRawTitle();
        this.rawAuthor = record.getRawAuthor();

        // 기록 내용
        this.sentence = record.getSentence();
        this.comment = record.getComment();

        // 상태 및 시간
        this.matchStatus = record.getMatchStatus();
        this.recordedAt = record.getRecordedAt();
        this.updatedAt = record.getUpdatedAt();
        this.matchedAt = record.getMatchedAt();
    }

    // Getters
    public Long getId() { return id; }
    public Long getUserId() { return userId; }
    public String getUsername() { return username; }
    public String getUserEmail() { return userEmail; }
    public Long getBookId() { return bookId; }
    public String getBookTitle() { return bookTitle; }
    public String getBookAuthor() { return bookAuthor; }
    public String getBookCoverUrl() { return bookCoverUrl; }
    public String getRawTitle() { return rawTitle; }
    public String getRawAuthor() { return rawAuthor; }
    public String getSentence() { return sentence; }
    public String getComment() { return comment; }
    public ReadingRecord.MatchStatus getMatchStatus() { return matchStatus; }
    public LocalDateTime getRecordedAt() { return recordedAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public LocalDateTime getMatchedAt() { return matchedAt; }
}