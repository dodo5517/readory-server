package me.dodo.readingnotes.dto.admin;

public class AdminRecordUpdateRequest {
    private String rawTitle;
    private String rawAuthor;
    private String sentence;
    private String comment;

    // Getters & Setters
    public String getRawTitle() { return rawTitle; }
    public void setRawTitle(String rawTitle) { this.rawTitle = rawTitle; }

    public String getRawAuthor() { return rawAuthor; }
    public void setRawAuthor(String rawAuthor) { this.rawAuthor = rawAuthor; }

    public String getSentence() { return sentence; }
    public void setSentence(String sentence) { this.sentence = sentence; }

    public String getComment() { return comment; }
    public void setComment(String comment) { this.comment = comment; }
}