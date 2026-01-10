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
    private AuthEventType eventType; // LOGIN, LOGIN_FAIL, LOGOUT_CURRENT_DEVICE, LOGOUT_ALL_DEVICES

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

    public enum AuthEventType { LOGIN, LOGIN_FAIL, LOGOUT_CURRENT_DEVICE, LOGOUT_ALL_DEVICES }
    public enum AuthResult { SUCCESS, FAIL }


    @PrePersist
    public void prePersist() {
        this.createdAt = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }

    public AuthEventType getEventType() { return eventType; }
    public void setEventType(AuthEventType eventType) { this.eventType = eventType; }

    public AuthResult getResult() { return result; }
    public void setResult(AuthResult result) { this.result = result; }

    public String getFailReason() { return failReason; }
    public void setFailReason(String failReason) { this.failReason = failReason; }

    public String getIpAddress() { return ipAddress; }
    public void setIpAddress(String ipAddress) { this.ipAddress = ipAddress; }

    public String getUserAgent() { return userAgent; }
    public void setUserAgent(String userAgent) { this.userAgent = userAgent; }

    public String getIdentifier() { return identifier; }
    public void setIdentifier(String identifier) { this.identifier = identifier; }

    public String getProvider() { return provider; }
    public void setProvider(String provider) { this.provider = provider; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
