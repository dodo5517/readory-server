package me.dodo.readingnotes.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "books", uniqueConstraints = {
        @UniqueConstraint(name = "uq_isbn13", columnNames = "isbn13")
})
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Book {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 255)
    private String title;

    @Column(nullable = false, length = 255)
    private String author;

    @Column(length = 255)
    private String publisher;

    @Column(length = 10)
    private String isbn10;

    @Column(length = 13)
    private String isbn13;

    private LocalDate publishedDate;

    @Column(length = 512)
    private String coverUrl;

    @Column(nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(nullable = false)
    private LocalDateTime updatedAt = LocalDateTime.now();

    private LocalDateTime deletedAt;

    public static Book createFrom(String title, String author, String publisher,
                                   String isbn10, String isbn13, String coverUrl, LocalDate publishedDate) {
        Book book = new Book();
        book.title = title;
        book.author = author;
        book.publisher = publisher;
        book.isbn10 = isbn10;
        book.isbn13 = isbn13;
        book.coverUrl = coverUrl;
        book.publishedDate = publishedDate;
        return book;
    }

    // 기존 책 메타데이터 갱신 (upsert 시 최신 정보로 반영)
    public void updateFrom(String title, String author, String publisher,
                            String isbn10, String isbn13, String coverUrl, LocalDate publishedDate) {
        this.title = title;
        this.author = author;
        this.publisher = publisher;
        this.isbn10 = isbn10;
        this.isbn13 = isbn13;
        this.coverUrl = coverUrl;
        this.publishedDate = publishedDate;
    }

    // 외부 API 재조회로 최신성을 확인했음을 기록 (필드 값이 그대로여도 갱신 시각을 갱신함)
    public void markRefreshed() {
        this.updatedAt = LocalDateTime.now();
    }

    public void markDeleted() {
        this.deletedAt = LocalDateTime.now();
    }

    public void restore() {
        this.deletedAt = null;
    }

    @PreUpdate
    public void preUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}
