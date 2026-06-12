package me.dodo.readingnotes.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

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

    private ClientHttpRequestFactory requestFactory() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(3000);
        factory.setReadTimeout(5000);
        return factory;
    }

    @Bean(name = "kakaoBookRestClient")
    public RestClient kakaoBookRestClient() {
        if (kakaoApiKey == null || kakaoApiKey.isBlank()) {
            throw new IllegalStateException("Kakao Book REST API 키가 비어있습니다.");
        }
        return RestClient.builder()
                .requestFactory(requestFactory())
                .baseUrl(kakaoBaseUrl)
                .defaultHeader(HttpHeaders.AUTHORIZATION, "KakaoAK " + kakaoApiKey)
                .build();
    }

    @Bean(name = "naverBookRestClient")
    public RestClient naverBookRestClient() {
        return RestClient.builder()
                .requestFactory(requestFactory())
                .baseUrl(naverBaseUrl)
                .defaultHeader("X-Naver-Client-Id", naverClientId)
                .defaultHeader("X-Naver-Client-Secret", naverClientSecret)
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .build();
    }
}
