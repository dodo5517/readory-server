package me.dodo.readingnotes.controller;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import me.dodo.readingnotes.dto.auth.AuthResponse;
import me.dodo.readingnotes.dto.auth.AuthResult;
import me.dodo.readingnotes.dto.auth.LoginRequest;
import me.dodo.readingnotes.dto.user.UserResponse;
import me.dodo.readingnotes.service.AuthService;
import me.dodo.readingnotes.util.CookieUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import me.dodo.readingnotes.util.JwtTokenProvider;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;
    private final JwtTokenProvider jwtTokenProvider;
    private static final Logger log = LoggerFactory.getLogger(AuthController.class);

    public AuthController(AuthService authService, JwtTokenProvider jwtTokenProvider) {
        this.authService = authService;
        this.jwtTokenProvider = jwtTokenProvider;
    }

    // 일반 로그인
    @PostMapping("/login")
    public AuthResponse loginUser(@RequestBody @Valid LoginRequest request,
                                  HttpServletRequest httpRequest,
                                  HttpServletResponse httpResponse) {
        log.debug("로그인 요청(request): {}", request.toString());

        // Header에서 User-Agent 가져옴
        String userAgent = httpRequest.getHeader("User-Agent");

        AuthResult result = authService.loginUser(
                request.getEmail(),
                request.getPassword(),
                userAgent
        );

        // refreshToken -> HttpOnly 쿠키에 저장
        ResponseCookie refreshCookie = CookieUtil.createRefreshTokenCookie(result.getRefreshToken());
        // 헤더에 저장
        httpResponse.addHeader("Set-Cookie", refreshCookie.toString());


        // accessToken -> JSON 응답 body에 포함
        // refreshToken은 쿠키로 보냈으므로 응답 body에는 null로 처리
        return new AuthResponse("로그인 성공", new UserResponse(result.getUser()), result.getAccessToken(),
                null, result.getExpiresIn(), result.getServerTime());
    }

    // 현재 기기에서 로그아웃
    @PostMapping("/logout")
    public ResponseEntity<Void> logout(HttpServletRequest httpRequest,
                                       HttpServletResponse httpResponse,
                                       @CookieValue(value = "refreshToken", required = false) String refreshToken) {

        log.debug("현재 기기에서 로그아웃 요청");
        
        // Header에서 User-Agent 가져옴
        String userAgent = httpRequest.getHeader("User-Agent");

        // JwtAuthFilter가 심어준 값 사용
        Long userId = (Long) httpRequest.getAttribute("USER_ID");

        if (userId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        authService.logoutUser(userId, userAgent);

        // refreshToken 쿠키 제거
        ResponseCookie deleteCookie = CookieUtil.deleteRefreshTokenCookie();

        // 헤더에 저장
        httpResponse.addHeader(HttpHeaders.SET_COOKIE, deleteCookie.toString());

        return ResponseEntity.noContent().build(); // 204 응답
    }

    // 모든 기기에서 로그아웃
    @PostMapping("/logout/all")
    public ResponseEntity<Void> logoutAllDevices(HttpServletRequest httpRequest,
                                                 HttpServletResponse httpResponse) {
        // 이미 만료된 쿠키로 요청이 올 수도 있으므로 @CookieValue는 굳이 받지 않음.

        log.debug("모든 기기에서 로그아웃 요청");

        // JwtAuthFilter가 심어준 값 사용
        Long userId = (Long) httpRequest.getAttribute("USER_ID");

        if (userId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        authService.logoutAllDevices(userId);

        // refreshToken 쿠키 제거
        ResponseCookie deleteCookie = CookieUtil.deleteRefreshTokenCookie();

        // 헤더에 저장
        httpResponse.addHeader(HttpHeaders.SET_COOKIE, deleteCookie.toString());

        return ResponseEntity.noContent().build(); // 204 응답
    }

    // 토큰 재발급
    @PostMapping("/reissue")
    public AuthResponse reissueUser(@CookieValue(value = "refreshToken", required = false) String refreshToken,
                                    HttpServletRequest httpRequest){
        log.debug("토큰 재발급 요청");

        // refresh token 유효성 검사
        if (refreshToken == null || refreshToken.isBlank()) {
            throw new IllegalArgumentException("refresh token이 쿠키에 존재하지 않습니다.");
        }

        // Header에서 User-Agent 가져옴
        String userAgent = httpRequest.getHeader("User-Agent");

        AuthResult result = authService.reissueAccessToken(refreshToken, userAgent);

        return new AuthResponse("토큰 재발급 성공", new UserResponse(result.getUser()), result.getAccessToken(),
                null, result.getExpiresIn(), result.getServerTime());
    }
}
