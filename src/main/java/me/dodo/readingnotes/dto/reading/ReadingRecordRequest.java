package me.dodo.readingnotes.dto.reading;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
public class ReadingRecordRequest {
    private String rawTitle;
    private String rawAuthor;
    private String sentence;
    private String comment;
    private LocalDateTime recordedAt;

    @Override
    public String toString() {
        return "ReadingRecord{" +
                ", title='" + rawTitle + '\'' +
                ", author='" + rawAuthor + '\'' +
                ", sentence='" + sentence + '\'' +
                ", comment='" + comment + '\'' +
                ", recordedAt='" + recordedAt + '\'' +
                '}';
    }
}
