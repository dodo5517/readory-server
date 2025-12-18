package me.dodo.readingnotes.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig {

    @Value("${frontend.url}")
    private String frontendUrl;

    @Bean
    public WebMvcConfigurer corsConfigurer() {
        return new WebMvcConfigurer() {
            @Override
            public void addCorsMappings(CorsRegistry registry) {
                registry.addMapping("/**") // 모든 경로에 대해
                        .allowedOriginPatterns(frontendUrl) // 프론트 도메인 허용
                        .allowedMethods("GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS") // 허용할 HTTP 메서드
                        .allowedHeaders("*") // 모든 헤더 허용
                        .allowCredentials(true); // 쿠키 등 인증 정보 허용

            }
        };
    }
}
