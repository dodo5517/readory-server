package me.dodo.readingnotes.dto.admin;

import me.dodo.readingnotes.domain.Book;

import java.time.LocalDate;
import java.time.LocalDateTime;

public record BookDetailResponse(
        Long id,
        String title,
        String author,
        String publisher,
        String isbn10,
        String isbn13,
        LocalDate publishedDate,
        String coverUrl,
        LocalDateTime createdAt,
        LocalDateTime updatedAt,
        LocalDateTime deletedAt
) {
    public static BookDetailResponse from(Book book) {
        return new BookDetailResponse(
                book.getId(),
                book.getTitle(),
                book.getAuthor(),
                book.getPublisher(),
                book.getIsbn10(),
                book.getIsbn13(),
                book.getPublishedDate(),
                book.getCoverUrl(),
                book.getCreatedAt(),
                book.getUpdatedAt(),
                book.getDeletedAt()
        );
    }
}
