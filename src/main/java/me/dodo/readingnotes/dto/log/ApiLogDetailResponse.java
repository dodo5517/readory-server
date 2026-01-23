package me.dodo.readingnotes.dto.log;

import me.dodo.readingnotes.domain.ApiLog;

import java.time.LocalDateTime;

public class ApiLogDetailResponse {

    private Long id;
    private Long userId;
    private String userRole;

    private String method;
    private String path;
    private String queryString;

    private int statusCode;
    private ApiLog.Result result;

    private String ipAddress;
    private String userAgent;

    private int executionTimeMs;

    private String errorCode;
    private String errorMessage;

    private LocalDateTime createdAt;

    public ApiLogDetailResponse(ApiLog log) {
        this.id = log.getId();
        this.userId = (log.getUser() == null) ? null : log.getUser().getId();
        this.userRole = log.getUserRole();
        this.method = log.getMethod();
        this.path = log.getPath();
        this.queryString = log.getQueryString();
        this.statusCode = log.getStatusCode();
        this.result = log.getResult();
        this.ipAddress = log.getIpAddress();
        this.userAgent = log.getUserAgent();
        this.executionTimeMs = log.getExecutionTimeMs();
        this.errorCode = log.getErrorCode();
        this.errorMessage = log.getErrorMessage();
        this.createdAt = log.getCreatedAt();
    }

    // getters
    public Long getId() { return id; }
    public Long getUserId() { return userId; }
    public String getUserRole() { return userRole; }
    public String getMethod() { return method; }
    public String getPath() { return path; }
    public String getQueryString() { return queryString; }
    public int getStatusCode() { return statusCode; }
    public ApiLog.Result getResult() { return result; }
    public String getIpAddress() { return ipAddress; }
    public String getUserAgent() { return userAgent; }
    public int getExecutionTimeMs() { return executionTimeMs; }
    public String getErrorCode() { return errorCode; }
    public String getErrorMessage() { return errorMessage; }
    public LocalDateTime getCreatedAt() { return createdAt; }

}
