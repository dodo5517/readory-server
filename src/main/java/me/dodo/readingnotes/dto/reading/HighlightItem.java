package me.dodo.readingnotes.dto.reading;

import me.dodo.readingnotes.domain.RecordHighlight;

/**
 * 하이라이트 응답 항목. sentence 기준 [start, end) 범위와 색.
 */
public record HighlightItem(Long id, int start, int end, String color) {

    public static HighlightItem from(RecordHighlight highlight) {
        return new HighlightItem(
                highlight.getId(),
                highlight.getStartOffset(),
                highlight.getEndOffset(),
                highlight.getColor().name()
        );
    }
}
