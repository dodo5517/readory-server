package me.dodo.readingnotes.dto.reading;

import java.time.LocalDateTime;
import java.util.List;

public class ReadingRecordItem {
    private Long id;
    private String sentence;
    private String comment;
    private LocalDateTime recordedAt;
    private List<HighlightItem> highlights;

    public ReadingRecordItem(Long id, LocalDateTime recordedAt, String sentence, String comment,
                             List<HighlightItem> highlights) {
        this.id = id;
        this.recordedAt = recordedAt;
        this.sentence = sentence;
        this.comment = comment;
        this.highlights = highlights;
    }

    // Getter
    public Long getId() { return id; }
    public String getSentence() { return sentence; }
    public String getComment() { return comment; }
    public LocalDateTime getRecordedAt() { return recordedAt; }
    public List<HighlightItem> getHighlights() { return highlights; }
}
