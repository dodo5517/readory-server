package me.dodo.readingnotes.dto.admin;


import me.dodo.readingnotes.domain.Book;

import java.time.LocalDate;
import java.time.LocalDateTime;

public class BookDetailResponse {
    private Long id;
    private String title;
    private String author;
    private String publisher;
    private String isbn10;
    private String isbn13;
    private LocalDate publishedDate;
    private String coverUrl;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private LocalDateTime deletedAt;

    public BookDetailResponse(Book book) {
        this.id = book.getId();
        this.title = book.getTitle();
        this.author = book.getAuthor();
        this.publisher = book.getPublisher();
        this.isbn10 = book.getIsbn10();
        this.isbn13 = book.getIsbn13();
        this.publishedDate = book.getPublishedDate();
        this.coverUrl = book.getCoverUrl();
        this.createdAt = book.getCreatedAt();
        this.updatedAt = book.getUpdatedAt();
        this.deletedAt = book.getDeletedAt();
    }

    // Getters
    public Long getId() { return id; }
    public String getTitle() { return title; }
    public String getAuthor() { return author; }
    public String getPublisher() { return publisher; }
    public String getIsbn10() { return isbn10; }
    public String getIsbn13() { return isbn13; }
    public LocalDate getPublishedDate() { return publishedDate; }
    public String getCoverUrl() { return coverUrl; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public LocalDateTime getDeletedAt() { return deletedAt; }
}
