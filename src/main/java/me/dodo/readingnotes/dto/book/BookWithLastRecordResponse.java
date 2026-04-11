package me.dodo.readingnotes.dto.book;

import java.time.LocalDateTime;

public class BookWithLastRecordResponse extends BookResponse {
    private LocalDateTime lastRecordAt;
    private Integer year;
    private boolean pinned;

    public BookWithLastRecordResponse(
            Long bookId, String title, String author, String isbn10,
            String isbn13, String coverUrl, LocalDateTime lastRecordAt,
            Integer year, boolean pinned) {
        super(bookId, title, author, isbn10, isbn13, coverUrl);
        this.lastRecordAt = lastRecordAt;
        this.year = year;
        this.pinned = pinned;
    }

    public LocalDateTime getLastRecordAt() { return lastRecordAt; }
    public Integer getYear() { return year; }
    public boolean isPinned() { return pinned; }
}