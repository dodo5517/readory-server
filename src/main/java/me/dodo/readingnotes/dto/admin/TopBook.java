package me.dodo.readingnotes.dto.admin;

public class TopBook {

    private final Long bookId;
    private final String title;
    private final String author;
    private final String coverUrl;
    private final long recordCount;

    public TopBook(Long bookId, String title, String author, String coverUrl, long recordCount) {
        this.bookId = bookId;
        this.title = title;
        this.author = author;
        this.coverUrl = coverUrl;
        this.recordCount = recordCount;
    }

    public Long getBookId() { return bookId; }
    public String getTitle() { return title; }
    public String getAuthor() { return author; }
    public String getCoverUrl() { return coverUrl; }
    public long getRecordCount() { return recordCount; }
}