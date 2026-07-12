package me.dodo.readingnotes.dto.book;

import java.time.LocalDateTime;

public record BookWithLastRecordResponse(
        Long id,
        String title,
        String author,
        String isbn10,
        String isbn13,
        String coverUrl,
        LocalDateTime lastRecordAt,
        Integer year,
        boolean pinned
) {}
