package me.dodo.readingnotes.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

import java.time.LocalDateTime;

@Entity
@Table(name = "user_auth_logs")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class UserAuthLog {

    @Id @GeneratedValue
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    @OnDelete(action = OnDeleteAction.CASCADE)
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

    public static UserAuthLog create(User user, String identifier, String provider,
                                      AuthEventType eventType, AuthResult result, String failReason,
                                      String ipAddress, String userAgent) {
        UserAuthLog log = new UserAuthLog();
        log.user = user;
        log.identifier = identifier;
        log.provider = provider;
        log.eventType = eventType;
        log.result = result;
        log.failReason = failReason;
        log.ipAddress = ipAddress;
        log.userAgent = userAgent;
        return log;
    }

    @PrePersist
    public void prePersist() {
        this.createdAt = LocalDateTime.now();
    }
}
