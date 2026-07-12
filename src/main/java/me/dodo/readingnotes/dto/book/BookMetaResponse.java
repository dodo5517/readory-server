package me.dodo.readingnotes.dto.book;

public record BookMetaResponse(
        Long id,
        String title,
        String author,
        String publisher,
        String publishedDate, // ISO 문자열 (예: "2025-08-10T14:22:31")
        String coverUrl,
        String periodStart,
        String periodEnd
) {}
