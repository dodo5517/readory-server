package me.dodo.readingnotes.external.client;

import me.dodo.readingnotes.dto.book.BookCandidate;
import me.dodo.readingnotes.external.BookSearchClient;
import me.dodo.readingnotes.external.adapter.KakaoBookAdapter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.List;
@Component
@Order(1)
public class KakaoBookClient implements BookSearchClient {
    private static final Logger log = LoggerFactory.getLogger(KakaoBookClient.class);
    private final WebClient webClient;
    private final KakaoBookAdapter adapter;

    public KakaoBookClient(
            @Qualifier("kakaoBookWebClient") WebClient webClient,
            KakaoBookAdapter adapter) {
        this.webClient = webClient;
        this.adapter = adapter;
    }

    @Override
    public List<BookCandidate> search(String rawTitle, String rawAuthor, int limit) {
        String query = buildQuery(rawTitle, rawAuthor);
        int size = normalizeLimit(limit);

        KakaoBookAdapter.KakaoResponse response = fetchFromApi(query, size);
        return adapter.adapt(response);
    }

    // 검색 쿼리 빌드
    private String buildQuery(String rawTitle, String rawAuthor) {
        return (rawAuthor == null || rawAuthor.isBlank())
                ? "\"" + rawTitle + "\""
                : "\"" + rawTitle + "\" \"" + rawAuthor + "\"";
    }

    // 카카오 API 허용 범위(1~50)로 정규화
    private int normalizeLimit(int limit) {
        return Math.min(Math.max(limit, 1), 50);
    }

    // 카카오 API 호출
    private KakaoBookAdapter.KakaoResponse fetchFromApi(String query, int size) {
        return webClient.get()
                .uri(uri -> uri.queryParam("query", query)
                        .queryParam("size", size)
                        .queryParam("sort", "accuracy")
                        .build())
                .retrieve()
                .onStatus(status -> status.isError(), r ->
                        r.bodyToMono(String.class).map(body ->
                                new RuntimeException("Kakao API error: " + r.statusCode() + " - " + body)))
                .bodyToMono(KakaoBookAdapter.KakaoResponse.class)
                .block();
    }
}