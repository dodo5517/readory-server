package me.dodo.readingnotes.dto.calendar;

import java.time.LocalDate;

public record DayStat(
        LocalDate date,
        long count,
        String coverUrl,
        int bookCount
) {}
