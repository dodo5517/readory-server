package me.dodo.readingnotes.config;

import me.dodo.readingnotes.repository.UserRepository;
import me.dodo.readingnotes.service.CustomOAuth2UserService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private final OAuth2SuccessHandler oAuth2SuccessHandler;
    private final UserRepository userRepository;
    private final JwtAuthFilter jwtAuthFilter;

    public SecurityConfig(OAuth2SuccessHandler oAuth2SuccessHandler,
                          UserRepository userRepository,
                          JwtAuthFilter jwtAuthFilter) {
        this.oAuth2SuccessHandler = oAuth2SuccessHandler;
        this.userRepository = userRepository;
        this.jwtAuthFilter = jwtAuthFilter;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http,
                                           CustomOAuth2UserService customOAuth2UserService) throws Exception {
        http
                .csrf(csrf -> csrf.disable()) // H2나 Postman 테스트용
                // H2 콘솔 띄우기 위해 옵션들 disable 처리
                .headers(headers -> headers.frameOptions(frame -> frame.disable()))
                // 이쪽 url들은 권한 없이 들어갈 수 있음
                .authorizeHttpRequests(auth -> auth
                    .requestMatchers("/**").permitAll() // .requestMatchers("/login/**", "/oauth2/**", "/auth/**").permitAll()
                    .requestMatchers("/records/me", "/records/me/**").authenticated()
                    .anyRequest().authenticated()
                )
                .oauth2Login(oauth -> oauth
                        .userInfoEndpoint(userInfo -> userInfo.userService(customOAuth2UserService))
                        .successHandler(oAuth2SuccessHandler)
                )
                .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);

        // API Key 필터, /records 로 시작하는 경로에만 적용함.
        ApiKeyFilter apiKeyFilter = new ApiKeyFilter(
                userRepository,
                "/records"
        );

        http.addFilterBefore(apiKeyFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}
