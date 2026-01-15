package me.dodo.readingnotes.dto.log;

import me.dodo.readingnotes.domain.UserAuthLog;

public class LogListResponse {
    private Long id;
    private Long userId;
    private UserAuthLog.AuthEventType eventType;
    private UserAuthLog.AuthResult result;
    private String ipAddress;
    private String createdAt;

    public LogListResponse(UserAuthLog userAuthLog) {
        this.id = userAuthLog.getId();
        this.userId = (userAuthLog.getUser() != null ? userAuthLog.getUser().getId() : null);
        this.eventType = userAuthLog.getEventType();
        this.result = userAuthLog.getResult();
        this.ipAddress = userAuthLog.getIpAddress();
        this.createdAt = userAuthLog.getCreatedAt().toString();
    }

    public Long getId() { return id; }
    public Long getUserId() { return userId; }
    public UserAuthLog.AuthEventType getEventType() { return eventType; }
    public UserAuthLog.AuthResult getResult() { return result; }
    public String getIpAddress() { return ipAddress; }
    public String getCreatedAt() { return createdAt; }

}
