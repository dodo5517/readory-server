package me.dodo.readingnotes.dto.reading;

import java.time.LocalDateTime;

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

    // 기본 생성자
    // 없으면 JPA와 동일하게 Jackson이 리플렉션을 못해서 자동으로 body에 있는 값을 객체 형태로 넣을 수 없게 됨.
    // 스프링은 내부적으로 Jackson이라는 JSON 변환기를 씀.
    public ReadingRecordRequest(){}


    public String getRawTitle() {
        return rawTitle;
    }
    public void setRawTitle(String rawTitle) {
        this.rawTitle = rawTitle;
    }

    public String getRawAuthor() {
        return rawAuthor;
    }
    public void setRawAuthor(String rawAuthor) {
        this.rawAuthor = rawAuthor;
    }

    public String getSentence() {
        return sentence;
    }
    public void setSentence(String sentence) {
        this.sentence = sentence;
    }

    public String getComment() {
        return comment;
    }
    public void setComment(String comment) {
        this.comment = comment;
    }

    public java.time.LocalDateTime getRecordedAt() {
        return recordedAt;
    }
    public void setRecordedAt(java.time.LocalDateTime recordedAt) {
        this.recordedAt = recordedAt;
    }

}
