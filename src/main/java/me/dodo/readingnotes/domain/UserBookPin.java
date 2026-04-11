package me.dodo.readingnotes.domain;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "user_book_pins",
        uniqueConstraints = {
                @UniqueConstraint(name = "uq_user_book_pin", columnNames = {"user_id", "book_id"})
        }
)
public class UserBookPin {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false, foreignKey = @ForeignKey(name = "fk_ubp_user"))
    @org.hibernate.annotations.OnDelete(action = org.hibernate.annotations.OnDeleteAction.CASCADE)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "book_id", nullable = false, foreignKey = @ForeignKey(name = "fk_ubp_book"))
    private Book book;

    @Column(nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    public UserBookPin() {}

    public UserBookPin(User user, Book book) {
        this.user = user;
        this.book = book;
    }

    @PrePersist
    public void prePersist() {
        if (createdAt == null) createdAt = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public User getUser() { return user; }
    public Book getBook() { return book; }
    public LocalDateTime getCreatedAt() { return createdAt; }
}