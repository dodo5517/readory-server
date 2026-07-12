package me.dodo.readingnotes.dto.book;

import me.dodo.readingnotes.domain.BookComment;

import java.time.LocalDateTime;

public record BookCommentResponse(
        Long id,
        String content,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
    public static BookCommentResponse from(BookComment comment) {
        return new BookCommentResponse(
                comment.getId(), comment.getContent(), comment.getCreatedAt(), comment.getUpdatedAt()
        );
    }
}
