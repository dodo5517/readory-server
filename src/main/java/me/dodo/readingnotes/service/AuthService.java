package me.dodo.readingnotes.service;

import jakarta.servlet.http.HttpServletRequest;
import me.dodo.readingnotes.domain.RefreshToken;
import me.dodo.readingnotes.domain.User;
import me.dodo.readingnotes.dto.auth.AuthResult;
import me.dodo.readingnotes.repository.RefreshTokenRepository;
import me.dodo.readingnotes.repository.UserRepository;
import me.dodo.readingnotes.util.DeviceInfoParser;
import me.dodo.readingnotes.util.JwtTokenProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.ZoneId;
import java.time.LocalDateTime;

import static me.dodo.readingnotes.util.RequestUtils.getCurrentHttpRequest;

@Service
public class AuthService {

    private final JwtTokenProvider jwtTokenProvider;
    private final RefreshTokenRepository refreshTokenRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final UserAuthLogService userAuthLogService;

    private static final Logger log = LoggerFactory.getLogger(AuthService.class);

    @Autowired
    public AuthService(JwtTokenProvider jwtTokenProvider,
                       RefreshTokenRepository refreshTokenRepository,
                       UserRepository userRepository,
                       PasswordEncoder passwordEncoder,
                       UserAuthLogService userAuthLogService) {
        this.jwtTokenProvider = jwtTokenProvider;
        this.refreshTokenRepository = refreshTokenRepository;
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.userAuthLogService = userAuthLogService;
    }

    // 유저 로그인(필요한 최소 정보만 따로 받음)
    @Transactional // 트랜젝션 처리
    public AuthResult loginUser(String email, String password, String userAgent) {
        // request 가져오기
        HttpServletRequest httpRequest = getCurrentHttpRequest();

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> {
                    userAuthLogService.logLoginFail(null, email, "LOCAL", "존재하지 않는 이메일입니다.", httpRequest);
                    return new IllegalArgumentException("존재하지 않는 이메일입니다.");
                });

        if(user.getUserStatus() == User.UserStatus.BLOCKED){
            userAuthLogService.logLoginFail(user, email, "LOCAL", "차단된 계정입니다.", httpRequest);
            throw new IllegalArgumentException("차단된 계정입니다.");
        }
        if(!passwordEncoder.matches(password,user.getPassword())){ // 평문 비교가 아닌 해시 비교
            userAuthLogService.logLoginFail(user, email, "LOCAL", "비밀번호가 일치하지 않습니다.", httpRequest);
            throw new IllegalArgumentException("비밀번호가 일치하지 않습니다.");
        }
        log.info("로그인 성공");

        // 디바이스 정보 파싱
        String deviceInfo = DeviceInfoParser.extractDeviceInfo(userAgent);
        log.debug("deviceInfo: {}", deviceInfo);

        // 토큰 생성
        String accessToken = jwtTokenProvider.createAccessToken(user);
        String refreshToken = jwtTokenProvider.createRefreshToken();


        // refresh 토큰 만료 시간의 Date 타입을 LocalDateTime으로 변환(로그인 하는 시점에 지정하므로 공통부분임)
        LocalDateTime refreshExpiry = jwtTokenProvider.getExpirationDate(refreshToken)
                .toInstant().atZone(ZoneId.of("Asia/Seoul")).toLocalDateTime();

        // DB에 저장(쿼리에서 update or insert)
        refreshTokenRepository.upsert(user.getId(), deviceInfo, refreshToken, refreshExpiry);

        // access 토큰 만료까지 남은 시간
        long expiresIn = jwtTokenProvider.getRemainingSeconds(accessToken);
        // 서버 시간
        long serverTime = System.currentTimeMillis();

        // 로컬 로그인 성공 로그 저장
        userAuthLogService.logLoginSuccess(user, email, user.getProvider(),httpRequest);

        return new AuthResult(user, accessToken, refreshToken, expiresIn, serverTime);
    }

    // 현재 기기에서 로그아웃(RefreshToken 삭제)
    @Transactional
    public void logoutUser(Long userId, String userAgent) {
        User user = userRepository.findById(userId).
            orElseThrow(()->new IllegalArgumentException("존재하지 않는 유저입니다."));

        // 디바이스 정보 파싱
        String deviceInfo = DeviceInfoParser.extractDeviceInfo(userAgent);
        log.debug("deviceInfo: {}", deviceInfo);

        // userId와 deviceInfo로 refreshToken 찾기
        RefreshToken token = refreshTokenRepository.findByUserIdAndDeviceInfo(userId, deviceInfo)
                .orElseThrow(() -> new IllegalArgumentException("Refresh token이 존재하지 않습니다."));

        // refreshToken DB에서 삭제
        refreshTokenRepository.delete(token);

        // 해당 기기 로그아웃 로그 저장
        HttpServletRequest httpRequest = getCurrentHttpRequest();
        userAuthLogService.logCurrentDeviceLogout(user, user.getEmail(), user.getProvider(), httpRequest);
    }

    // 모든 기기에서 로그아웃
    @Transactional
    public void logoutAllDevices(Long userId) {
        User user = userRepository.findById(userId).
                orElseThrow(()->new IllegalArgumentException("존재하지 않는 유저입니다."));

        // 해당 유저의 모든 디바이스의 RefreshToken 삭제
        refreshTokenRepository.deleteAllByUserId(userId);

        // 모든 기기 로그아웃 로그 저장
        HttpServletRequest httpRequest = getCurrentHttpRequest();
        userAuthLogService.logAllDeviceLogout(user, user.getEmail(), user.getProvider(), httpRequest);
    }

    // 토큰 재발급
    @Transactional
    public AuthResult reissueAccessToken(String refreshToken, String userAgent) {
        // 토큰 유효성 검증
        if (!jwtTokenProvider.validateToken(refreshToken)) {
            throw new IllegalArgumentException("유효하지 않거나 만료된 refresh_token 입니다.");
        }

        // DB에서 토큰 조회
        RefreshToken tokenInDb = refreshTokenRepository.findByToken(refreshToken)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 refresh_token 입니다."));

        // 디바이스 정보 파싱
        String deviceInfo = DeviceInfoParser.extractDeviceInfo(userAgent);
        log.debug("deviceInfo: {}", deviceInfo);
        
        // DB에서 device_info 검증
        if (!tokenInDb.getDeviceInfo().equals(deviceInfo)) {
            throw new IllegalArgumentException("기기 정보가 일치하지 않습니다.");
        }

        User user = tokenInDb.getUser();
        String newAccessToken = jwtTokenProvider.createAccessToken(user);

        // access 토큰 만료까지 남은 시간
        long expiresIn = jwtTokenProvider.getRemainingSeconds(newAccessToken);
        // 서버 시간
        long serverTime = System.currentTimeMillis();

        // 필요 시 refreshToken 재발급
        return new AuthResult(user, newAccessToken, refreshToken, expiresIn, serverTime);
    }
}