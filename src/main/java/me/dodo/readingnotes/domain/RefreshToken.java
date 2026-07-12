package me.dodo.readingnotes.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
// 복합 유니크 제약 조건으로 같은 디바이스 중복 방지, 다른 디바이스는 새 토큰 발급
@Table(name = "refresh_tokens",
    uniqueConstraints = @UniqueConstraint(columnNames = {"user_id", "device_info"})
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class RefreshToken {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY) // LAZY: 지연 로딩
    @JoinColumn(name = "user_id", nullable = false)
    @org.hibernate.annotations.OnDelete(action = org.hibernate.annotations.OnDeleteAction.CASCADE) // CASCADE 설정
    private User user;

    @Column(nullable = false, unique = true, length = 512)
    private String token; // refresh_token

    @Column(name = "device_info", length = 255)
    private String deviceInfo; // 로그인한 장치 ex. PC, 모바일, 태블릿

    @Column(name = "expiry_date", nullable = false)
    private LocalDateTime expiryDate; // 만료 날짜

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now(); // 만들어진 날짜시간

    @PrePersist
    public void prePersist() {
        this.createdAt = LocalDateTime.now();
    }

    public RefreshToken(User user, String token, String deviceInfo, LocalDateTime expiryDate) {
        this.user = user;
        this.token = token;
        this.deviceInfo = deviceInfo;
        this.expiryDate = expiryDate;
    }
}
