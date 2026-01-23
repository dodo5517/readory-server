package me.dodo.readingnotes.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    private final ApiLogInterceptor apiLogInterceptor;

    public WebConfig(ApiLogInterceptor apiLogInterceptor) {
        this.apiLogInterceptor = apiLogInterceptor;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(apiLogInterceptor)
                .addPathPatterns("/**");
    }

    // CORS 설정은 SecurityConfig에서 처리하므로 여기서는 제거
}