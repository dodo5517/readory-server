package me.dodo.readingnotes.util;

import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import jakarta.servlet.http.HttpServletRequest;
import me.dodo.readingnotes.domain.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.security.Key;
import java.util.Date;

@Component // static 메서드만 있으면 필요 없긴 함.
public class JwtTokenProvider {

    private static final Logger log = LoggerFactory.getLogger(JwtTokenProvider.class);

    // 나중에 application.properties로 옮겨야 함.
    // 시크릿 키 HS256은 대칭형 알고리즘임.
    private final Key key = Keys.secretKeyFor(SignatureAlgorithm.HS256);

    // 토큰 유효 시간
    // 30분 =  1000 * 60 * 30;
    // 테스트 5초 = 1000 * 5 * 1;
    private final long accessTokenValidity = 1000 * 60 * 30;
    // 7일
    private final long refreshTokenValidity = 1000L * 60 * 60 * 24 * 7;

    // Acess Token 생성
    public String createAccessToken(User user) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + accessTokenValidity);

        return Jwts.builder()
                .setSubject(user.getEmail()) // 토큰 주인 email
                .claim("userId", user.getId()) // 토큰 주인 ID
                .claim("role", user.getRole()) // 토큰 주인 Role
                .setIssuedAt(now) // 발급 시간
                .setExpiration(expiryDate) // 만료 시간
                .signWith(key) // 서명(변조 방지)
                .compact(); // 최종 문자열로 변환
    }

    // Refresh Token 생성
    public String createRefreshToken() {
        Date now = new Date();
        Date expiry = new Date(now.getTime() + refreshTokenValidity);
        // access token 새로 받을 때만 사용하므로 이메일 담지 않음.
        // DB에 저장해서 나중에 비교할 때 사용함.
        return Jwts.builder()
                .setIssuedAt(now)
                .setExpiration(expiry)
                .signWith(key)
                .compact();
    }

    // 클라이언트에서 보낸 토큰에서 userId 추출
    public Long getUserIdFromToken(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(key)
                .build()
                .parseClaimsJws(token)
                .getBody()
                .get("userId", Long.class);
    }

    // 토큰 유효성 검사
    public boolean validateToken(String token) {
        try {
            Jwts.parserBuilder()
                    .setSigningKey(key)
                    .build()
                    .parseClaimsJws(token); // 예외 없이 파싱되면 유효함.
                return true;
        } catch (ExpiredJwtException e) {
            log.error("만료된 토큰입니다: {}",e.getMessage());
        } catch (JwtException e) {
            log.error("유효하지 않은 토큰입니다: {}",e.getMessage());
        }
        return false;
    }
    public void assertValid(String token) {
        // 예외를 삼키지 말고 그대로 던진다 (ExpiredJwtException 등)
        Jwts.parserBuilder().setSigningKey(key).build().parseClaimsJws(token);
    }

    // Authorization 헤더에서 꺼내기
    public static String extractToken(HttpServletRequest request) {
        String authHeader = request.getHeader("Authorization");

        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            throw new IllegalArgumentException("Authorization header가 없거나 Bearer 타입이 아닙니다.");
        }

        String token = authHeader.substring(7).trim();

        if (token.isBlank() || token.equalsIgnoreCase("null")) {
            throw new IllegalArgumentException("Bearer 토큰이 비어있거나 null 입니다.");
        }

        return token;
    }

    // 토큰 만료 시간(Date) 확인
    public Date getExpirationDate(String token){
        return Jwts.parserBuilder()
                .setSigningKey(key)
                .build()
                .parseClaimsJws(token)
                .getBody()
                .getExpiration();
    }

    // 토큰 만료 남은 시간 확인
    public long getRemainingSeconds(String token) {
        Date exp = getExpirationDate(token);
        long now = System.currentTimeMillis();
        long remain = exp.getTime() - now;
        return Math.max(0, remain / 1000); // 초 단위
    }

    // Role 추출
    public String getRoleFromToken(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(key)
                .build()
                .parseClaimsJws(token)
                .getBody()
                .get("role", String.class);
    }
}
