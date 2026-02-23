package me.dodo.readingnotes.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import me.dodo.readingnotes.domain.User;
import me.dodo.readingnotes.repository.UserRepository;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;
import java.util.Optional;

public class ApiKeyFilter extends OncePerRequestFilter {

    public static final String ATTR_API_USER_ID = "apiUserId";
    public static final String HEADER_API_KEY = "X-Api-Key";

    private final UserRepository userRepository;

    // 정확히 이 경로일 때만 검사
    private final String protectedPath;

    public ApiKeyFilter(UserRepository userRepository, String protectedPath) {
        this.userRepository = userRepository;
        this.protectedPath = protectedPath;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getServletPath();
        // "/records" 정확 일치일 때만 필터 동작. 그 외 경로는 전부 PASS.
        return !protectedPath.equals(path);
    }
    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {
        String apiKey = request.getHeader(HEADER_API_KEY);

        if (apiKey == null || apiKey.isBlank()) {
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "X-Api-Key가 누락되었습니다.");
            return;
        }

        Optional<User> userOpt = userRepository.findByApiKey(apiKey);
        if (userOpt.isEmpty()) {
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "잘못된 API Key 입니다.");
            return;
        }

        // SecurityContext에 인증 정보 set
        UsernamePasswordAuthenticationToken authentication =
                new UsernamePasswordAuthenticationToken(userOpt.get(), null, List.of(new SimpleGrantedAuthority(userOpt.get().getRole())));

        SecurityContextHolder.getContext().setAuthentication(authentication);

        request.setAttribute(ATTR_API_USER_ID, userOpt.get().getId());
        chain.doFilter(request, response);
    }
}