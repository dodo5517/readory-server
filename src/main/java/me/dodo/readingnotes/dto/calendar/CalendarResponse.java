package me.dodo.readingnotes.dto.calendar;

import java.time.LocalDate;
import java.util.List;

public record CalendarResponse(
        LocalDate rangeStart,
        LocalDate rangeEndExclusive,
        List<DayStat> days,
        CalendarSummary summary
) {}
