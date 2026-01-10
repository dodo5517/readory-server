package me.dodo.readingnotes.domain;

import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "user_auth_logs")
public class UserAuthLog {

    @Id @GeneratedValue
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user; // 실패 시 null 가능

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AuthEventType eventType; // LOGIN, LOGOUT, LOGIN_FAIL

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AuthResult result; // SUCCESS, FAIL

    @Column(length = 50)
    private String failReason;

    @Column(length = 45)
    private String ipAddress;

    @Column(length = 255)
    private String userAgent;

    @Column(length = 255)
    private String identifier;   // email, socialId
    @Column(length = 20)
    private String provider;     // LOCAL, GOOGLE, KAKAO, NAVER

    @Column(nullable = false)
    private LocalDateTime createdAt;

    public enum AuthEventType { LOGIN, LOGOUT, LOGIN_FAIL }
    public enum AuthResult { SUCCESS, FAIL }

    @PrePersist
    public void prePersist() {
        this.createdAt = LocalDateTime.now();
    }
}
