package me.dodo.readingnotes.dto.reading;

import java.time.LocalDateTime;

public class ReadingRecordItem {
    private Long id;
    private String sentence;
    private String comment;
    private LocalDateTime recordedAt;

    public ReadingRecordItem(Long id, LocalDateTime recordedAt, String sentence, String comment) {
        this.id = id;
        this.recordedAt = recordedAt;
        this.sentence = sentence;
        this.comment = comment;
    }

    // Getter
    public Long getId() { return id; }
    public String getSentence() { return sentence; }
    public String getComment() { return comment; }
    public LocalDateTime getRecordedAt() { return recordedAt; }
}
