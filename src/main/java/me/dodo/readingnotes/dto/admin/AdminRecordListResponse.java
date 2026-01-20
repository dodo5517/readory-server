package me.dodo.readingnotes.dto.admin;

import me.dodo.readingnotes.domain.ReadingRecord;
import java.time.LocalDateTime;

public class AdminRecordListResponse {
    private Long id;
    private String username;
    private String rawTitle;
    private String rawAuthor;
    private String sentence;
    private ReadingRecord.MatchStatus matchStatus;
    private LocalDateTime recordedAt;

    public AdminRecordListResponse(ReadingRecord record) {
        this.id = record.getId();
        this.username = record.getUser().getUsername();
        this.rawTitle = record.getRawTitle();
        this.rawAuthor = record.getRawAuthor();
        this.sentence = truncate(record.getSentence(), 100);
        this.matchStatus = record.getMatchStatus();
        this.recordedAt = record.getRecordedAt();
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
    public String getSentence() { return sentence; }
    public ReadingRecord.MatchStatus getMatchStatus() { return matchStatus; }
    public LocalDateTime getRecordedAt() { return recordedAt; }
}