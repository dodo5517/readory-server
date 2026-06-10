package me.dodo.readingnotes.util;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;

@Component
public class CookieUtil {

    private final boolean cookieSecure;

    public CookieUtil(@Value("${cookie.secure}") boolean cookieSecure) {
        this.cookieSecure = cookieSecure;
    }

    public ResponseCookie createRefreshTokenCookie(String refreshToken) {
        return ResponseCookie.from("refreshToken", refreshToken)
                .httpOnly(true)
                .secure(cookieSecure)  // 운영 시에는 true로 해야 함
                .path("/") // 모든 경로에 쿠키 전송
                .maxAge(30L * 24 * 60 * 60) // 30일
                .sameSite("Strict") // CSRF 방지
                .build();
    }

    public ResponseCookie deleteRefreshTokenCookie() {
        return ResponseCookie.from("refreshToken", "")
                .httpOnly(true)
                .secure(cookieSecure)
                .path("/")
                .maxAge(0)
                .sameSite("Strict")
                .build();
    }
}
