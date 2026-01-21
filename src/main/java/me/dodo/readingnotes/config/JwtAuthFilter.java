package me.dodo.readingnotes.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import me.dodo.readingnotes.util.JwtTokenProvider;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

@Component
public class JwtAuthFilter extends OncePerRequestFilter {

    private static final String ATTR_USER_ID = "USER_ID";
    private static final String ATTR_USER_ROLE = "USER_ROLE";

    private final JwtTokenProvider jwtTokenProvider;

    public JwtAuthFilter(JwtTokenProvider jwtTokenProvider) {
        this.jwtTokenProvider = jwtTokenProvider;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain)
            throws ServletException, IOException {
        // CORS Preflight 요청은 그냥 통과
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            filterChain.doFilter(request, response);
            return;
        }

        try {
            String accessToken = jwtTokenProvider.extractToken(request);

            if (accessToken != null && jwtTokenProvider.validateToken(accessToken)) {
                Long userId = jwtTokenProvider.getUserIdFromToken(accessToken);
                String role = jwtTokenProvider.getRoleFromToken(accessToken);

                //  로그 / 인터셉터 / 컨트롤러 공통 사용
                request.setAttribute(ATTR_USER_ID, userId);
                request.setAttribute(ATTR_USER_ROLE, role);
                // SecurityContext에 인증 정보 설정
                UsernamePasswordAuthenticationToken authentication =
                        new UsernamePasswordAuthenticationToken(
                                userId,                                          // principal
                                null,                                            // credentials
                                List.of(new SimpleGrantedAuthority("ROLE_" + role))  // authorities
                        );

                SecurityContextHolder.getContext().setAuthentication(authentication);
            }

        } catch (Exception e) {
            // 인증 실패여도 throw 하면 안 됨, 로그는 실패 요청도 남겨야 함
        }

        filterChain.doFilter(request, response);
    }
}
