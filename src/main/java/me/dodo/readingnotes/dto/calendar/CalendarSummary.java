package me.dodo.readingnotes.dto.calendar;

public record CalendarSummary(
        int totalDaysWithRecord,
        long totalRecords,
        String firstRecordedAt, // YYYY-MM-DD
        String lastRecordedAt   // YYYY-MM-DD
) {}
