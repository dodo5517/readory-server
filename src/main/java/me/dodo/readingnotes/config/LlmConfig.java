package me.dodo.readingnotes.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

@Configuration
@ConditionalOnProperty(name = "external.llm.provider", havingValue = "api", matchIfMissing = true)
public class LlmConfig {

    @Value("${external.anthropic.base-url}")
    private String baseUrl;

    @Value("${external.anthropic.api-key}")
    private String apiKey;

    @Value("${external.anthropic.version}")
    private String anthropicVersion;

    @Value("${external.anthropic.connect-timeout}")
    private int connectTimeout;

    @Value("${external.anthropic.read-timeout}")
    private int readTimeout;

    private ClientHttpRequestFactory requestFactory() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(connectTimeout);
        factory.setReadTimeout(readTimeout); // 엮기는 오래 걸리므로 길게
        return factory;
    }

    @Bean(name = "anthropicRestClient")
    public RestClient anthropicRestClient() {
        if (apiKey == null || apiKey.isBlank()) {
            throw new IllegalStateException("Anthropic API 키가 비어있습니다.");
        }
        return RestClient.builder()
                .requestFactory(requestFactory())
                .baseUrl(baseUrl)
                .defaultHeader("x-api-key", apiKey)
                .defaultHeader("anthropic-version", anthropicVersion)
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .build();
    }
}