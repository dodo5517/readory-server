package me.dodo.readingnotes.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration
public class BookConfig {

    @Value("${external.kakao.book.base-url}")
    private String kakaoBaseUrl;

    @Value("${external.kakao.book.rest-api-key}")
    private String kakaoApiKey;

    @Value("${external.naver.book.base-url}")
    private String naverBaseUrl;

    @Value("${spring.security.oauth2.client.registration.naver.client-id}")
    private String naverClientId;

    @Value("${spring.security.oauth2.client.registration.naver.client-secret}")
    private String naverClientSecret;

    // 스프링 컨테이너에 WebClient 빈을 등록함
    // 나중에 @Qualifier("kakaoBookWebClient") 사용하여 이 빈을 주입할 수 있음.

    // 카카오 책 검색 API WebClient
    @Bean(name = "kakaoBookWebClient")
    public WebClient kakaoBookWebClient() {
        if (kakaoApiKey == null || kakaoApiKey.isBlank()) {
            throw new IllegalStateException("Kakao Book REST API 키가 비어있습니다.");
        }
        return WebClient.builder()
                .baseUrl(kakaoBaseUrl)
                .defaultHeader(HttpHeaders.AUTHORIZATION, "KakaoAK " + kakaoApiKey)
                .build();
    }

    // 네이버 책 검색 API WebClient
    @Bean(name = "naverBookWebClient")
    public WebClient naverBookWebClient() {
        return WebClient.builder()
                .baseUrl(naverBaseUrl)
                .defaultHeader("X-Naver-Client-Id", naverClientId)
                .defaultHeader("X-Naver-Client-Secret", naverClientSecret)
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .build();
    }
}
