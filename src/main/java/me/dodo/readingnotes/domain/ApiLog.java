package me.dodo.readingnotes.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

import java.time.LocalDateTime;

@Entity
@Table(name = "api_logs")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ApiLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    @OnDelete(action = OnDeleteAction.CASCADE)
    private User user; // 실패 시 null 가능

    private String userRole;

    @Column(length = 100)
    private String method;

    @Column(length = 255)
    private String path;

    @Column(length = 255)
    private String queryString;

    @Column(length = 20)
    private int statusCode;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ApiLog.Result result; // SUCCESS, FAIL

    @Column(length = 45)
    private String ipAddress;

    @Column(length = 255)
    private String userAgent;

    @Column(nullable = false, length = 255)
    private int executionTimeMs;

    @Column(length = 20)
    private String errorCode;

    @Column(length = 100)
    private String errorMessage;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    public enum Result { SUCCESS, FAIL }

    public static ApiLog create(User user, String userRole, String method, String path, String queryString,
                                 int statusCode, Result result, String ipAddress, String userAgent,
                                 int executionTimeMs, String errorCode, String errorMessage) {
        ApiLog log = new ApiLog();
        log.user = user;
        log.userRole = userRole;
        log.method = method;
        log.path = path;
        log.queryString = queryString;
        log.statusCode = statusCode;
        log.result = result;
        log.ipAddress = ipAddress;
        log.userAgent = userAgent;
        log.executionTimeMs = executionTimeMs;
        log.errorCode = errorCode;
        log.errorMessage = errorMessage;
        return log;
    }

    @PrePersist
    public void prePersist() {
        this.createdAt = LocalDateTime.now();
    }
}
