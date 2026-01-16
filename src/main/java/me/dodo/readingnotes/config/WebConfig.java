package me.dodo.readingnotes.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig {

    private final ApiLogInterceptor apiLogInterceptor;

    public WebConfig(ApiLogInterceptor apiLogInterceptor) {
        this.apiLogInterceptor = apiLogInterceptor;
    }

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

            @Override
            public void addInterceptors(InterceptorRegistry registry) {
                // 사용자 정의 인터셉터를 추가하고 적용할 경로를 지정
                registry.addInterceptor(apiLogInterceptor)
                        .addPathPatterns("/**"); //모든 요청에 적용
            }
        };
    }
}
