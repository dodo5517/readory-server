package me.dodo.readingnotes.dto.reading;

import java.time.LocalDateTime;

public class ReadingRecordItem {
    private Long id;
    private String sentence;
    private String comment;
    private LocalDateTime createdAt;

    public ReadingRecordItem(Long id, LocalDateTime createdAt, String sentence, String comment) {
        this.id = id;
        this.createdAt = createdAt;
        this.sentence = sentence;
        this.comment = comment;
    }

    // Getter
    public Long getId() { return id; }
    public String getSentence() { return sentence; }
    public String getComment() { return comment; }
    public LocalDateTime getCreatedAt() { return createdAt; }
}
