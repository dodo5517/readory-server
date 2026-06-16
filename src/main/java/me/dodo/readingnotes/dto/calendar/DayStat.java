package me.dodo.readingnotes.dto.calendar;

import java.time.LocalDate;

public class DayStat {
    private LocalDate date;
    private long count;
    private String coverUrl;

    public DayStat(LocalDate date, long count) {
        this.date = date;
        this.count = count;
    }

    public DayStat(LocalDate date, long count, String coverUrl) {
        this.date = date;
        this.count = count;
        this.coverUrl = coverUrl;
    }

    public LocalDate getDate() { return date; }
    public long getCount() { return count; }
    public String getCoverUrl() { return coverUrl; }
}
