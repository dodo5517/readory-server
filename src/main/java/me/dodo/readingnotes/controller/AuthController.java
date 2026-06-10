package me.dodo.readingnotes.controller;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import me.dodo.readingnotes.dto.auth.AuthResponse;
import me.dodo.readingnotes.dto.auth.AuthResult;
import me.dodo.readingnotes.dto.auth.LoginRequest;
import me.dodo.readingnotes.dto.common.ApiResponse;
import me.dodo.readingnotes.dto.user.UserResponse;
import me.dodo.readingnotes.exception.AuthException;
import me.dodo.readingnotes.service.AuthService;
import me.dodo.readingnotes.util.CookieUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;
    private final CookieUtil cookieUtil;
    private static final Logger log = LoggerFactory.getLogger(AuthController.class);

    public AuthController(AuthService authService, CookieUtil cookieUtil) {
        this.authService = authService;
        this.cookieUtil = cookieUtil;
    }

    // 일반 로그인
    @PostMapping("/login")
    public ResponseEntity<ApiResponse<AuthResponse>> loginUser(@RequestBody @Valid LoginRequest request,
                                                               HttpServletRequest httpRequest,
                                                               HttpServletResponse httpResponse) {
        log.debug("로그인 요청(request): {}", request.toString());

        String userAgent = httpRequest.getHeader("User-Agent");

        AuthResult result = authService.loginUser(
                request.getEmail(),
                request.getPassword(),
                userAgent
        );

        ResponseCookie refreshCookie = cookieUtil.createRefreshTokenCookie(result.getRefreshToken());
        httpResponse.addHeader("Set-Cookie", refreshCookie.toString());

        AuthResponse authResponse = new AuthResponse("로그인 성공", new UserResponse(result.getUser()),
                result.getAccessToken(), null, result.getExpiresIn(), result.getServerTime());

        return ResponseEntity.ok(ApiResponse.success("로그인 성공", authResponse));
    }

    // 현재 기기에서 로그아웃
    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<Void>> logout(HttpServletRequest httpRequest,
                                                     HttpServletResponse httpResponse) {
        log.debug("현재 기기에서 로그아웃 요청");

        String userAgent = httpRequest.getHeader("User-Agent");
        Long userId = (Long) httpRequest.getAttribute("USER_ID");

        if (userId == null) {
            throw new AuthException("인증이 필요합니다.");
        }

        authService.logoutUser(userId, userAgent);

        ResponseCookie deleteCookie = cookieUtil.deleteRefreshTokenCookie();
        httpResponse.addHeader(HttpHeaders.SET_COOKIE, deleteCookie.toString());

        return ResponseEntity.ok(ApiResponse.success("로그아웃 되었습니다."));
    }

    // 모든 기기에서 로그아웃
    @PostMapping("/logout/all")
    public ResponseEntity<ApiResponse<Void>> logoutAllDevices(HttpServletRequest httpRequest,
                                                               HttpServletResponse httpResponse) {
        log.debug("모든 기기에서 로그아웃 요청");

        Long userId = (Long) httpRequest.getAttribute("USER_ID");

        if (userId == null) {
            throw new AuthException("인증이 필요합니다.");
        }

        authService.logoutAllDevices(userId);

        ResponseCookie deleteCookie = cookieUtil.deleteRefreshTokenCookie();
        httpResponse.addHeader(HttpHeaders.SET_COOKIE, deleteCookie.toString());

        return ResponseEntity.ok(ApiResponse.success("모든 기기에서 로그아웃 되었습니다."));
    }

    // 토큰 재발급
    @PostMapping("/reissue")
    public ResponseEntity<ApiResponse<AuthResponse>> reissueUser(
            @CookieValue(value = "refreshToken", required = false) String refreshToken,
            HttpServletRequest httpRequest) {
        log.debug("토큰 재발급 요청");

        String userAgent = httpRequest.getHeader("User-Agent");

        AuthResult result = authService.reissueAccessToken(refreshToken, userAgent);

        AuthResponse authResponse = new AuthResponse("토큰 재발급 성공", new UserResponse(result.getUser()),
                result.getAccessToken(), null, result.getExpiresIn(), result.getServerTime());

        return ResponseEntity.ok(ApiResponse.success("토큰 재발급 성공", authResponse));
    }
}
