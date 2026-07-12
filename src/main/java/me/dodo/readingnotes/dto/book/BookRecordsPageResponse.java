package me.dodo.readingnotes.dto.book;

import me.dodo.readingnotes.dto.reading.ReadingRecordItem;

import java.util.List;

public record BookRecordsPageResponse(
        BookMetaResponse book,
        BookCommentResponse bookComment, // 책 전체 코멘트 (없으면 null)
        List<ReadingRecordItem> content,
        String nextCursor,
        boolean hasMore
) {}
