package me.dodo.readingnotes.service;

import jakarta.servlet.http.HttpServletRequest;
import me.dodo.readingnotes.domain.User;
import me.dodo.readingnotes.domain.UserAuthLog;
import me.dodo.readingnotes.repository.AuthLogRepository;
import me.dodo.readingnotes.util.RequestInfoExtractor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthLogService {
    private final AuthLogRepository authLogRepository;
    private final RequestInfoExtractor requestInfoExtractor;
    private static final Logger log = LoggerFactory.getLogger(AuthLogService.class);

    public AuthLogService(AuthLogRepository authLogRepository,
                          RequestInfoExtractor requestInfoExtractor) {
        this.authLogRepository = authLogRepository;
        this.requestInfoExtractor = requestInfoExtractor;
    }

    // 로그인 성공 로그 저장
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void logLoginSuccess(User user, String identifier, String provider, HttpServletRequest request) {
        saveSafely(user, identifier, provider, UserAuthLog.AuthEventType.LOGIN, UserAuthLog.AuthResult.SUCCESS, null, request);
    }

    // 해당 기기 로그아웃 로그 저장
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void logCurrentDeviceLogout(User user, String identifier, String provider, HttpServletRequest request) {
        saveSafely(user, identifier, provider, UserAuthLog.AuthEventType.LOGOUT_CURRENT_DEVICE, UserAuthLog.AuthResult.SUCCESS, null, request);
    }

    // 모든 기기 로그아웃 로그 저장
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void logAllDeviceLogout(User user, String identifier, String provider, HttpServletRequest request) {
        saveSafely(user, identifier, provider, UserAuthLog.AuthEventType.LOGOUT_ALL_DEVICES, UserAuthLog.AuthResult.SUCCESS, null, request);
    }

    // 로그인 실패 로그 저장
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void logLoginFail(User user,
                             String identifier,
                             String provider,
                             String reason,
                             HttpServletRequest request) {

        try {
            UserAuthLog log = new UserAuthLog();
            if (user != null) { log.setUser(user); }
            else { log.setUser(null); }
            log.setIdentifier(identifier);
            log.setProvider(provider);
            log.setEventType(UserAuthLog.AuthEventType.LOGIN_FAIL);
            log.setResult(UserAuthLog.AuthResult.FAIL);
            log.setFailReason(reason);

            if (request != null) {
                log.setIpAddress(requestInfoExtractor.extractIp(request));
                log.setUserAgent(requestInfoExtractor.extractUserAgent(request));
            }

            authLogRepository.save(log);
        } catch (Exception e) {
            // 인증 흐름 보호
            log.warn("Failed to log login failure", e);
        }
    }

    // 저장
    private void saveSafely(User user,
                            String identifier,
                            String provider,
                            UserAuthLog.AuthEventType eventType,
                            UserAuthLog.AuthResult result,
                            String failReason,
                            HttpServletRequest request) {
        try {
            UserAuthLog log = new UserAuthLog();

            log.setUser(user);
            log.setIdentifier(identifier);
            log.setProvider(provider);
            log.setEventType(eventType);
            log.setResult(result);
            log.setFailReason(failReason);

            if (request != null) {
                log.setIpAddress(requestInfoExtractor.extractIp(request));
                log.setUserAgent(requestInfoExtractor.extractUserAgent(request));
            }

            authLogRepository.save(log);
        } catch (Exception e) {
            log.warn("Failed to save user auth log", e);
        }
    }
}