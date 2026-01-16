package me.dodo.readingnotes.dto.log;

import me.dodo.readingnotes.domain.ApiLog;

public class ApiLogCommand {

    private final Long userId;              // null 가능
    private final String userRole;          // null/blank 가능

    private final String method;
    private final String path;
    private final String queryString;

    private final int statusCode;
    private final ApiLog.Result result;     // SUCCESS / FAIL

    private final String ipAddress;
    private final String userAgent;
    private final int executionTimeMs;

    private final String errorCode;         // null 가능
    private final String errorMessage;      // null 가능

    public ApiLogCommand(Long userId,
                         String userRole,
                         String method,
                         String path,
                         String queryString,
                         int statusCode,
                         ApiLog.Result result,
                         String ipAddress,
                         String userAgent,
                         int executionTimeMs,
                         String errorCode,
                         String errorMessage) {
        this.userId = userId;
        this.userRole = userRole;
        this.method = method;
        this.path = path;
        this.queryString = queryString;
        this.statusCode = statusCode;
        this.result = result;
        this.ipAddress = ipAddress;
        this.userAgent = userAgent;
        this.executionTimeMs = executionTimeMs;
        this.errorCode = errorCode;
        this.errorMessage = errorMessage;
    }

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
}