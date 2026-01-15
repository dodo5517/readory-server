package me.dodo.readingnotes.dto.log;

import me.dodo.readingnotes.domain.UserAuthLog;

public class LogDetailResponse {
    private Long id;
    private Long userId;
    private UserAuthLog.AuthEventType eventType;
    private UserAuthLog.AuthResult result;
    private String failResponse;
    private String ipAddress;
    private String userAgent;
    private String identifier;
    private String createdAt;

    public LogDetailResponse(UserAuthLog userAuthLog) {
        this.id = userAuthLog.getId();
        this.userId = (userAuthLog.getUser() != null ? userAuthLog.getUser().getId() : null);
        this.eventType = userAuthLog.getEventType();
        this.result = userAuthLog.getResult();
        this.failResponse = userAuthLog.getFailReason();
        this.ipAddress = userAuthLog.getIpAddress();
        this.userAgent = userAuthLog.getUserAgent();
        this.identifier = userAuthLog.getIdentifier();
        this.createdAt = userAuthLog.getCreatedAt().toString();
    }

    public Long getId() { return id; }
    public Long getUserId() { return userId; }
    public UserAuthLog.AuthEventType getEventType() { return eventType; }
    public UserAuthLog.AuthResult getResult() { return result; }
    public String getFailResponse() { return failResponse; }
    public String getIpAddress() { return ipAddress; }
    public String getUserAgent() { return userAgent; }
    public String getIdentifier() { return identifier; }
    public String getCreatedAt() { return createdAt; }
}
