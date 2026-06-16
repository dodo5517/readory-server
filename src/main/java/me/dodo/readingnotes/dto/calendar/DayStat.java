package me.dodo.readingnotes.dto.calendar;

import java.time.LocalDate;

public class DayStat {
    private final LocalDate date;
    private final long count;
    private String coverUrl;
    private int bookCount;

    public DayStat(LocalDate date, long count) {
        this.date = date;
        this.count = count;
    }

    public DayStat(LocalDate date, long count, String coverUrl) {
        this.date = date;
        this.count = count;
        this.coverUrl = coverUrl;
    }

    public DayStat(LocalDate date, long count, String coverUrl, int bookCount) {
        this.date = date;
        this.count = count;
        this.coverUrl = coverUrl;
        this.bookCount = bookCount;
    }

    public LocalDate getDate() { return date; }
    public long getCount() { return count; }
    public String getCoverUrl() { return coverUrl; }
    public int getBookCount() { return bookCount; }
}
