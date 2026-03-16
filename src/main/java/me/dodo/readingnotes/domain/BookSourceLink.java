package me.dodo.readingnotes.domain;

import jakarta.persistence.*;
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

    // 기본 생성자(JPA 필수)
    public BookSourceLink() {
    }

    // Getter / Setter
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Book getBook() { return book; }
    public void setBook(Book book) { this.book = book; }

    public String getSource() { return source; }
    public void setSource(String source) { this.source = source; }

    public String getExternalId() { return externalId; }
    public void setExternalId(String externalId) { this.externalId = externalId; }

    public String getIsbn10() { return isbn10; }
    public void setIsbn10(String isbn10) { this.isbn10 = isbn10; }

    public String getIsbn13() { return isbn13; }
    public void setIsbn13(String isbn13) { this.isbn13 = isbn13; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public String getMetaJson() { return metaJson; }
    public void setMetaJson(String metaJson) { this.metaJson = metaJson; }
}
