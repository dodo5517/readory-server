package me.dodo.readingnotes.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 완성된 독후감. 유저+책당 하나.
 * - content: 마크다운 한 덩어리(## 소제목 + 본문). 목차는 이 마크다운의 ## 헤딩을 파싱해 생성.
 * - title:   독후감 제목(개요에서 생성, 사용자 수정 가능).
 */
@Entity
@Table(name = "reflections",
        uniqueConstraints = {
                @UniqueConstraint(name = "uq_reflection_user_book", columnNames = {"user_id", "book_id"})
        }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Reflection {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false, foreignKey = @ForeignKey(name = "fk_reflection_user"))
    @org.hibernate.annotations.OnDelete(action = org.hibernate.annotations.OnDeleteAction.CASCADE)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "book_id", nullable = false, foreignKey = @ForeignKey(name = "fk_reflection_book"))
    private Book book;

    @Column(nullable = false, length = 500)
    private String title;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String content; // 마크다운

    @Column(nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(nullable = false)
    private LocalDateTime updatedAt = LocalDateTime.now();

    public static Reflection create(User user, Book book, String title, String content) {
        Reflection reflection = new Reflection();
        reflection.user = user;
        reflection.book = book;
        reflection.title = title;
        reflection.content = content;
        return reflection;
    }

    public void updateTitle(String title) {
        this.title = title;
    }

    public void updateContent(String content) {
        this.content = content;
    }

    @PrePersist
    public void prePersist() {
        if (createdAt == null) createdAt = LocalDateTime.now();
        if (updatedAt == null) updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    public void preUpdate() {
        updatedAt = LocalDateTime.now();
    }
}