package me.dodo.readingnotes.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.web.client.RestClient;

import java.util.List;

@Configuration
public class BookConfig {

    @Value("${external.kakao.book.base-url}")
    private String kakaoBaseUrl;

    @Value("${external.kakao.book.rest-api-key}")
    private String kakaoApiKey;

    @Value("${external.nlk.book.base-url}")
    private String nlkBaseUrl;

    @Value("${external.nlk.book.cert-key}")
    private String nlkCertKey;

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

    @Bean(name = "nlkBookRestClient")
    public RestClient nlkBookRestClient() {
        if (nlkCertKey == null || nlkCertKey.isBlank()) {
            throw new IllegalStateException("국립중앙도서관(SEOJI) 인증키가 비어있습니다.");
        }
        // SEOJI result_style=json 응답은 Content-Type이 application/json이 아닐 수 있어(text/plain 등)
        // Jackson 컨버터가 해당 미디어타입도 JSON으로 역직렬화하도록 확장한다.
        MappingJackson2HttpMessageConverter jacksonConverter = new MappingJackson2HttpMessageConverter();
        jacksonConverter.setSupportedMediaTypes(List.of(
                MediaType.APPLICATION_JSON,
                MediaType.valueOf("text/javascript"),
                MediaType.valueOf("application/x-javascript"),
                MediaType.TEXT_PLAIN,
                MediaType.TEXT_HTML
        ));
        return RestClient.builder()
                .requestFactory(requestFactory())
                .baseUrl(nlkBaseUrl)
                .messageConverters(converters -> {
                    converters.removeIf(c -> c instanceof MappingJackson2HttpMessageConverter);
                    converters.add(jacksonConverter);
                })
                .build();
    }
}
