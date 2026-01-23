package me.dodo.readingnotes.config;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

@Component
public class ApiLogRequestInfoExtractor {

    public Long extractUserIdOrNull(HttpServletRequest request) {
        // userId 추출
        Long userId = (Long) request.getAttribute("USER_ID");
        if (userId == null) {
            return null; // 없으면 null
        }
        return userId;
    }

    public String extractUserRoleOrNull(HttpServletRequest request) {
        Object attr = request.getAttribute("USER_ROLE");
        if (attr instanceof String && !((String) attr).isBlank()) {
            return (String) attr;
        }

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) return null;

        // 권한 여러 개면 첫 번째만 저장
        if (authentication.getAuthorities() != null && !authentication.getAuthorities().isEmpty()) {
            return authentication.getAuthorities().iterator().next().getAuthority();
        }
        return null;
    }

    public String extractPath(HttpServletRequest request) {
        // 컨텍스트 패스가 있는 경우를 고려하면 requestURI가 가장 안전
        return request.getRequestURI();
    }

    public String extractQueryString(HttpServletRequest request) {
        return request.getQueryString(); // 없으면 null
    }

    public String extractUserAgent(HttpServletRequest request) {
        return request.getHeader("User-Agent");
    }

    public String extractClientIp(HttpServletRequest request) {
        // 프록시/Cloudflare 환경이면 X-Forwarded-For 우선
        String xff = request.getHeader("X-Forwarded-For");
        if (xff != null && !xff.isBlank()) {
            // "client, proxy1, proxy2" 형태일 수 있음 → 첫 번째가 원 IP
            return xff.split(",")[0].trim();
        }

        String realIp = request.getHeader("X-Real-IP");
        if (realIp != null && !realIp.isBlank()) return realIp.trim();

        return request.getRemoteAddr();
    }

    public String extractErrorCode(Exception ex, int statusCode) {
        if (ex == null) return null;

        // 기본 fallback
        return "HTTP_" + statusCode;
    }

    public String extractErrorMessage(Exception ex, int statusCode) {
        if (ex == null) return null;

        // 메시지 그대로 저장하면 민감정보가 섞일 수 있어 최소화(운영 관점)
        String msg = ex.getMessage();
        if (msg == null || msg.isBlank()) return "HTTP_" + statusCode;
        return msg;
    }
}