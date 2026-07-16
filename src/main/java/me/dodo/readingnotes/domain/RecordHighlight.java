package me.dodo.readingnotes.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 기록 문장 일부에 대한 하이라이트(형광펜). 한 기록에 여러 개 가능.
 * - startOffset/endOffset: sentence 문자열 기준 [start, end) 범위. JS·Java 모두 UTF-16 코드유닛 기준이라 인덱스가 일치한다.
 * - color: 마커 색(전역 토큰과 매핑). 현재 GREEN/PEACH 2종.
 */
@Entity
@Table(name = "record_highlights",
        indexes = {
                @Index(name = "idx_rh_record", columnList = "reading_record_id")
        })
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class RecordHighlight {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "reading_record_id", nullable = false,
            foreignKey = @ForeignKey(name = "fk_rh_record"))
    @org.hibernate.annotations.OnDelete(action = org.hibernate.annotations.OnDeleteAction.CASCADE)
    private ReadingRecord record;

    // sentence 기준 시작 위치(포함)
    @Column(name = "start_offset", nullable = false)
    private int startOffset;

    // sentence 기준 끝 위치(미포함)
    @Column(name = "end_offset", nullable = false)
    private int endOffset;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private HighlightColor color;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    public enum HighlightColor { GREEN, PEACH }

    public static RecordHighlight create(ReadingRecord record, int startOffset, int endOffset, HighlightColor color) {
        RecordHighlight highlight = new RecordHighlight();
        highlight.record = record;
        highlight.startOffset = startOffset;
        highlight.endOffset = endOffset;
        highlight.color = color;
        return highlight;
    }

    @PrePersist
    public void prePersist() {
        if (createdAt == null) createdAt = LocalDateTime.now();
    }
}
