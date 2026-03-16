package me.dodo.readingnotes.repository;

import me.dodo.readingnotes.domain.RefreshToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;

public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {
    Optional<RefreshToken> findByToken(String token);
    Optional<RefreshToken> findByUserIdAndDeviceInfo(Long userId, String deviceInfo);

    @Modifying
    @Transactional
    @Query(value = """
        INSERT INTO refresh_tokens (user_id, device_info, token, expiry_date)
        VALUES (:userId, :deviceInfo, :token, :expiryDate)
        ON CONFLICT(user_id, device_info) DO UPDATE SET
            token = excluded.token,
            expiry_date = excluded.expiry_date
        """, nativeQuery = true)
    int upsert(@Param("userId") Long userId,
               @Param("deviceInfo") String deviceInfo,
               @Param("token") String token,
               @Param("expiryDate") LocalDateTime expiryDate);

    void deleteAllByUserId(Long userId);
}
