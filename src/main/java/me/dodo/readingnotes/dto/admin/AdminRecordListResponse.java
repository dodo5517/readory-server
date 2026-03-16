package me.dodo.readingnotes.dto.admin;

import me.dodo.readingnotes.domain.ReadingRecord;
import java.time.LocalDateTime;

public class AdminRecordListResponse {
    private Long id;
    private String username;
    private String rawTitle;
    private String rawAuthor;
    private ReadingRecord.MatchStatus matchStatus;
    private LocalDateTime createdAt;

    public AdminRecordListResponse(ReadingRecord record) {
        this.id = record.getId();
        this.username = record.getUser().getUsername();
        this.rawTitle = record.getRawTitle();
        this.rawAuthor = record.getRawAuthor();
        this.matchStatus = record.getMatchStatus();
        this.createdAt = record.getCreatedAt();
    }

    private String truncate(String str, int maxLength) {
        if (str == null) return null;
        return str.length() > maxLength ? str.substring(0, maxLength) + "..." : str;
    }

    // Getters
    public Long getId() { return id; }
    public String getUsername() { return username; }
    public String getRawTitle() { return rawTitle; }
    public String getRawAuthor() { return rawAuthor; }
    public ReadingRecord.MatchStatus getMatchStatus() { return matchStatus; }
    public LocalDateTime getCreatedAt() { return createdAt; }
}