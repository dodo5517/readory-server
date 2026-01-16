package me.dodo.readingnotes.dto.log;

import me.dodo.readingnotes.domain.ApiLog;

import java.time.LocalDateTime;

public class ApiLogListResponse {


    private Long id;
    private LocalDateTime createdAt;

    private String method;
    private String path;

    private int statusCode;
    private ApiLog.Result result;

    private Integer executionTimeMs;

    private Long userId;
    private String userRole;


    public ApiLogListResponse(ApiLog log) {
        this.id = log.getId();
        this.createdAt = log.getCreatedAt();
        this.method = log.getMethod();
        this.path = log.getPath();
        this.statusCode = log.getStatusCode();
        this.result = log.getResult();
        this.executionTimeMs = log.getExecutionTimeMs();
        this.userId = log.getUser() == null ? null : log.getUser().getId();
        this.userRole = log.getUserRole();
    }

    // getters
    public Long getId() { return id; }
    public Long getUserId() { return userId; }
    public String getUserRole() { return userRole; }
    public String getMethod() { return method; }
    public String getPath() { return path; }
    public int getStatusCode() { return statusCode; }
    public ApiLog.Result getResult() { return result; }
    public int getExecutionTimeMs() { return executionTimeMs; }
    public LocalDateTime getCreatedAt() { return createdAt; }

}
