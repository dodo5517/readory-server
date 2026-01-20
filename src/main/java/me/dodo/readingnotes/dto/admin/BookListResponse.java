package me.dodo.readingnotes.dto.admin;


import me.dodo.readingnotes.domain.Book;

import java.time.LocalDateTime;

public class BookListResponse {
    private Long id;
    private String title;
    private String author;
    private String publisher;
    private String coverUrl;
    private LocalDateTime createdAt;

    public BookListResponse(Book book) {
        this.id = book.getId();
        this.title = book.getTitle();
        this.author = book.getAuthor();
        this.publisher = book.getPublisher();
        this.coverUrl = book.getCoverUrl();
        this.createdAt = book.getCreatedAt();
    }

    // Getters
    public Long getId() { return id; }
    public String getTitle() { return title; }
    public String getAuthor() { return author; }
    public String getPublisher() { return publisher; }
    public String getCoverUrl() { return coverUrl; }
    public LocalDateTime getCreatedAt() { return createdAt; }
}
