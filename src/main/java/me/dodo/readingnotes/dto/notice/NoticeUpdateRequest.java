package me.dodo.readingnotes.dto.notice;

public class NoticeUpdateRequest {
    private String message;
    private Boolean enabled;

    public NoticeUpdateRequest() {}

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
    public Boolean getEnabled() { return enabled; }
    public void setEnabled(Boolean enabled) { this.enabled = enabled; }
}