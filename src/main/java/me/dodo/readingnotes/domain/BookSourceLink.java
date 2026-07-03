package me.dodo.readingnotes.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "book_source_links",
        uniqueConstraints = {
                @UniqueConstraint(name = "uq_book_source", columnNames = {"book_id", "source"}),
                @UniqueConstraint(name = "uq_source_external", columnNames = {"source", "external_id"})
        },
        indexes = {
                @Index(name = "idx_bsl_isbn13", columnList = "isbn13")
        }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class BookSourceLink {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "book_id", nullable = false,
            foreignKey = @ForeignKey(name = "fk_bsl_book"))
    private Book book;

    @Column(nullable = false, length = 20)
    private String source; // "KAKAO" / "NAVER" / "GOOGLE"

    @Column(name = "external_id", length = 512)
    private String externalId;

    @Column(length = 10)
    private String isbn10;

    @Column(length = 13)
    private String isbn13;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(columnDefinition = "TEXT")
    private String metaJson;

    public static BookSourceLink create(Book book, String source, String externalId, String isbn10, String isbn13) {
        BookSourceLink link = new BookSourceLink();
        link.book = book;
        link.source = source;
        link.externalId = externalId;
        link.isbn10 = isbn10;
        link.isbn13 = isbn13;
        return link;
    }

    // 기존 링크 재매칭 시 책/ISBN 갱신
    public void relinkBook(Book book, String isbn10, String isbn13) {
        this.book = book;
        this.isbn10 = isbn10;
        this.isbn13 = isbn13;
    }

    public void attachMetaJson(String metaJson) {
        this.metaJson = metaJson;
    }

    @PrePersist
    public void prePersist() {
        this.createdAt = LocalDateTime.now();
    }

    @PreUpdate
    public void preUpdate() {
        this.createdAt = LocalDateTime.now();
    }
}
