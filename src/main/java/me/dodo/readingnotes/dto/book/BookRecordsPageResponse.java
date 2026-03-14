package me.dodo.readingnotes.dto.book;

import me.dodo.readingnotes.dto.reading.ReadingRecordItem;

import java.util.List;

public class BookRecordsPageResponse {
    private BookMetaResponse book;
    private BookCommentResponse bookComment; // 책 전체 코멘트 (없으면 null)
    private List<ReadingRecordItem> content;
    private String nextCursor;
    private boolean hasMore;

    public BookRecordsPageResponse(BookMetaResponse book, BookCommentResponse bookComment,
                                   List<ReadingRecordItem> content, String nextCursor, boolean hasMore) {
        this.book = book;
        this.bookComment = bookComment;
        this.content = content;
        this.nextCursor = nextCursor;
        this.hasMore = hasMore;
    }

    // Getter
    public BookMetaResponse getBook() { return book; }
    public BookCommentResponse getBookComment() { return bookComment; }
    public List<ReadingRecordItem> getContent() { return content; }
    public String getNextCursor() { return nextCursor; }
    public boolean getHasMore() { return hasMore; }
}