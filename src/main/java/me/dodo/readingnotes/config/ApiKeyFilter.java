package me.dodo.readingnotes.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import me.dodo.readingnotes.domain.User;
import me.dodo.readingnotes.repository.UserRepository;
import me.dodo.readingnotes.util.ApiErrorWriter;
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
    private final ObjectMapper objectMapper;
    private final String protectedPath;

    public ApiKeyFilter(UserRepository userRepository, ObjectMapper objectMapper, String protectedPath) {
        this.userRepository = userRepository;
        this.objectMapper = objectMapper;
        this.protectedPath = protectedPath;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getServletPath();
        return !protectedPath.equals(path);
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {
        String apiKey = request.getHeader(HEADER_API_KEY);

        if (apiKey == null || apiKey.isBlank()) {
            ApiErrorWriter.writeApiError(response, objectMapper, 401, "API_KEY_MISSING", "X-Api-Key가 누락되었습니다.");
            return;
        }

        Optional<User> userOpt = userRepository.findByApiKey(apiKey);
        if (userOpt.isEmpty()) {
            ApiErrorWriter.writeApiError(response, objectMapper, 401, "API_KEY_INVALID", "잘못된 API Key 입니다.");
            return;
        }

        // 차단된 계정은 API Key로도 접근 불가
        if (userOpt.get().getUserStatus() == User.UserStatus.BLOCKED) {
            ApiErrorWriter.writeApiError(response, objectMapper, 403, "ACCOUNT_BLOCKED", "차단된 계정입니다.");
            return;
        }

        UsernamePasswordAuthenticationToken authentication =
                new UsernamePasswordAuthenticationToken(userOpt.get(), null,
                        List.of(new SimpleGrantedAuthority(userOpt.get().getRole())));

        SecurityContextHolder.getContext().setAuthentication(authentication);

        request.setAttribute(ATTR_API_USER_ID, userOpt.get().getId());
        request.setAttribute("apiUser", userOpt.get());
        chain.doFilter(request, response);
    }
}
