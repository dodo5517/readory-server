package me.dodo.readingnotes.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name= "users")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 50)
    private String username; // 유저 이름

    @Column(nullable = false, unique = true, length = 100)
    private String email; // 이메일

    @Column(length = 255)
    private String password; // 비밀번호 (소셜 로그인은 필요X)

    @Column(nullable = false, length = 20)
    private String provider; // ex.카카오, 네이버, 구글, 일반

    @Column(name = "provider_id", length = 100)
    private String providerId; // 소셜 로그인 시 인증id (일반 로그인은 필요X)

    @Column(name = "api_key", nullable = false, unique = true, length = 100)
    private String apiKey; // api_key, 모두 있어야 함

    @Column(name = "profile_image_url", length = 500)
    private String profileImageUrl; // 프로필 사진

    @Enumerated(EnumType.STRING)
    @Column(name = "user_status", nullable = false)
    private UserStatus userStatus = UserStatus.ACTIVE; // 차단/활동 여부, 기본값 = ACTIVE

    @Column(nullable = false, length = 20)
    private String role = "USER"; // 역할(권한)

    @CreationTimestamp // 엔티티 인스턴스가 생성될 때 자동으로 현재 시간 입력
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt; // 생성된 시간

    @UpdateTimestamp // 엔티티 인스턴스가 수정할 때 자동으로 현재 시간 입력
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt; // 수정된 시간


    public enum UserStatus { ACTIVE, BLOCKED, SUSPENDED }

    @PrePersist
    public void prePersist() {
        this.createdAt = LocalDateTime.now();
    }

    @Override // toString 예쁘게 보기 위해 오버라이딩
    public String toString() {
        return "User{" +
                "email='" + email + '\'' +
                ", username='" + username + '\'' +
                '}';
    }

    public static User createLocal(String email, String username, String encodedPassword, String apiKey) {
        User user = new User();
        user.email = email;
        user.username = username;
        user.password = encodedPassword;
        user.apiKey = apiKey;
        user.provider = "local";
        user.role = "USER";
        return user;
    }

    public static User fromSocial(String email, String username, String provider, String providerId, String apiKey) {
        User user = new User();
        user.email = email;
        user.username = username;
        user.provider = provider;
        user.providerId = providerId;
        user.apiKey = apiKey;
        user.role = "USER";
        return user;
    }

    public void changePassword(String encodedPassword) {
        this.password = encodedPassword;
    }

    public void changeUsername(String username) {
        this.username = username;
    }

    public void changeProfileImage(String url) {
        this.profileImageUrl = url;
    }

    public void changeStatus(UserStatus status) {
        this.userStatus = status;
    }

    public void changeRole(String role) {
        this.role = role;
    }

    public void reissueApiKey(String newApiKey) {
        this.apiKey = newApiKey;
    }

}
