package me.dodo.readingnotes.dto.book;

import me.dodo.readingnotes.domain.Book;

public record BookResponse(
        Long id,
        String title,
        String author,
        String isbn10,
        String isbn13,
        String coverUrl
) {
    public static BookResponse from(Book book) {
        return new BookResponse(
                book.getId(), book.getTitle(), book.getAuthor(),
                book.getIsbn10(), book.getIsbn13(), book.getCoverUrl()
        );
    }
}
