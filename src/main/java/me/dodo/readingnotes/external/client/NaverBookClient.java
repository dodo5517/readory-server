package me.dodo.readingnotes.external.client;


import me.dodo.readingnotes.dto.book.BookCandidate;
import me.dodo.readingnotes.external.BookSearchClient;
import me.dodo.readingnotes.external.adapter.NaverBookAdapter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.List;

@Component
@Order(2)
public class NaverBookClient implements BookSearchClient {
    private static final Logger log = LoggerFactory.getLogger(NaverBookClient.class);
    private final WebClient webClient;
    private final NaverBookAdapter adapter;

    public NaverBookClient(
            @Qualifier("naverBookWebClient") WebClient webClient,
            NaverBookAdapter adapter) {
        this.webClient = webClient;
        this.adapter = adapter;
    }

    @Override
    public List<BookCandidate> search(String rawTitle, String rawAuthor, int limit) {
        String query = buildQuery(rawTitle, rawAuthor);
        int display = normalizeLimit(limit);

        NaverBookAdapter.NaverResponse response = fetchFromApi(query, display);
        return adapter.adapt(response);
    }

    // 검색 쿼리 빌드
    private String buildQuery(String rawTitle, String rawAuthor) {
        if (rawAuthor == null || rawAuthor.isBlank()) {
            return rawTitle;
        }
        return rawTitle + " " + rawAuthor;
    }

    // Naver API 허용 범위(1~100)로 정규화
    private int normalizeLimit(int limit) {
        return Math.min(Math.max(limit, 1), 100);
    }

    // Naver API 호출
    private NaverBookAdapter.NaverResponse fetchFromApi(String query, int display) {
        return webClient.get()
                .uri(uri -> uri.queryParam("query", query)
                        .queryParam("display", display)
                        .queryParam("sort", "sim")  // 정확도순 정렬
                        .build())
                .retrieve()
                .onStatus(status -> status.isError(), r ->
                        r.bodyToMono(String.class).map(body ->
                                new RuntimeException("Naver API error: " + r.statusCode() + " - " + body)))
                .bodyToMono(NaverBookAdapter.NaverResponse.class)
                .block();
    }
}
