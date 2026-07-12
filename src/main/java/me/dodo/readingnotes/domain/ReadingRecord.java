package me.dodo.readingnotes.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity // 이 클래스가 JPA 엔티티임을 선언. DB 테이블과 매핑됨
@Table(name = "reading_records", // DB에서 이 엔티티가 매핑될 테이블 이름을 지정함
    indexes = {
            // user_id로 먼저 좁히고 recorded_at으로 정렬(기본이 desc임)
            @Index(name = "idx_rr_user_recorded", columnList = "user_id, recorded_at"),
            @Index(name = "idx_record_user_book_at_id", columnList = "user_id, book_id, recorded_at, id")
    })
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ReadingRecord {

    @Id //이 필드(id)가 기본 키임을 나타냄
    //기본 키의 값을 DB가 자동 증가(Auto Increment)로 생성하도록 지정함
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 관계: 책
    @ManyToOne(fetch = FetchType.LAZY) // 다(기록):1(책) 설정
    @JoinColumn(name= "book_id", nullable = true) // 외래 키로 book 테이블의 id를 참조함.
    private Book book;

    // 관계: 유저
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name= "user_id", nullable = false) // 외래키 user_id
    @org.hibernate.annotations.OnDelete(action = org.hibernate.annotations.OnDeleteAction.CASCADE) // CASCADE 설정
    private User user;

    @Column(length = 1000)
    private String sentence;
    // 출처 제거 전 원문 보관(nullable)
    @Column(name = "sentence_original", nullable = true, length = 1000)
    private String sentenceOriginal;
    @Column(length = 1000)
    private String comment;

    // 매칭 전 임시 원문 보관
    @Column(name = "raw_title", nullable = true, length = 255)
    private String rawTitle;
    @Column(name = "raw_author", nullable = true, length = 255)
    private String rawAuthor;

    @Enumerated(EnumType.STRING)
    @Column(name = "match_status", nullable = false) // 책 매칭된 상태
    private MatchStatus matchStatus = MatchStatus.PENDING;

    // 최초 기록된 시간
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    // 독서 기록 시각 (사용자가 설정하거나 생성 시각으로 초기화됨)
    @Column(name = "recorded_at", nullable = false)
    private LocalDateTime recordedAt;

    // 수정한 시간
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    // 책 매칭된 시간
    @Column(name = "matched_at")
    private LocalDateTime matchedAt;

    public enum MatchStatus { PENDING, RESOLVED_AUTO, RESOLVED_MANUAL, NO_CANDIDATE, MULTIPLE_CANDIDATES }

    public static ReadingRecord create(User user, String sentence, String sentenceOriginal, String comment,
                                        String rawTitle, String rawAuthor, LocalDateTime recordedAt) {
        ReadingRecord record = new ReadingRecord();
        record.user = user;
        record.sentence = sentence;
        record.sentenceOriginal = sentenceOriginal;
        record.comment = comment;
        record.rawTitle = rawTitle;
        record.rawAuthor = rawAuthor;
        record.recordedAt = recordedAt != null ? recordedAt : LocalDateTime.now();
        return record;
    }

    // 책 매칭(수동/자동)
    public void matchBook(Book book, MatchStatus status) {
        this.book = book;
        this.matchStatus = status;
        this.matchedAt = LocalDateTime.now();
    }

    // 책 매칭 해제
    public void unmatchBook() {
        this.book = null;
        this.matchStatus = MatchStatus.PENDING;
        this.matchedAt = LocalDateTime.now();
    }

    // null이 아닌 필드만 반영
    public void updateContent(String rawTitle, String rawAuthor, String sentence, String comment, LocalDateTime recordedAt) {
        if (rawTitle != null) this.rawTitle = rawTitle;
        if (rawAuthor != null) this.rawAuthor = rawAuthor;
        if (sentence != null) this.sentence = sentence;
        if (comment != null) this.comment = comment;
        if (recordedAt != null) this.recordedAt = recordedAt;
    }

    // entity가 db에 insert 되기 전에 호출됨.
    @PrePersist
    public void prePersist() {
        if (createdAt == null) createdAt = LocalDateTime.now();
        if (recordedAt == null) recordedAt = LocalDateTime.now();
        if (updatedAt == null) updatedAt = LocalDateTime.now();
        if (matchStatus == null) matchStatus = MatchStatus.PENDING;
    }

    @PreUpdate
    public void preUpdate() {
        updatedAt = LocalDateTime.now();
    }

    @Override
    public String toString() {
        return "ReadingRecord{" +
                "id=" + id +
                ", title='" + rawTitle + '\'' +
                ", author='" + rawAuthor + '\'' +
                ", sentence='" + sentence + '\'' +
                ", comment='" + comment + '\'' +
                ", matchStatus=" + matchStatus +
                ", createdAt=" + createdAt +
                '}';
    }
}
