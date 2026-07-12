package me.dodo.readingnotes.dto.admin;

import me.dodo.readingnotes.domain.Book;

import java.time.LocalDateTime;

public record BookListResponse(
        Long id,
        String title,
        String author,
        String publisher,
        String coverUrl,
        LocalDateTime createdAt
) {
    public static BookListResponse from(Book book) {
        return new BookListResponse(
                book.getId(),
                book.getTitle(),
                book.getAuthor(),
                book.getPublisher(),
                book.getCoverUrl(),
                book.getCreatedAt()
        );
    }
}
