package me.dodo.readingnotes.external.client;

import me.dodo.readingnotes.dto.book.BookCandidate;
import me.dodo.readingnotes.external.BookSearchClient;
import me.dodo.readingnotes.external.adapter.NlkBookAdapter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.nio.charset.StandardCharsets;
import java.util.List;

@Component
@Order(2)
public class NlkBookClient implements BookSearchClient {
    private static final Logger log = LoggerFactory.getLogger(NlkBookClient.class);

    private final RestClient restClient;
    private final NlkBookAdapter adapter;
    private final String certKey;

    public NlkBookClient(
            @Qualifier("nlkBookRestClient") RestClient restClient,
            NlkBookAdapter adapter,
            @Value("${external.nlk.book.cert-key}") String certKey) {
        this.restClient = restClient;
        this.adapter = adapter;
        this.certKey = certKey;
    }

    @Override
    public List<BookCandidate> search(String rawTitle, String rawAuthor, int limit) {
        int pageSize = normalizeLimit(limit);
        String title = normalizeTitleForQuery(rawTitle);
        String author = normalizeAuthorForQuery(rawAuthor);

        // 1차: 제목+작가
        if (author != null) {
            List<BookCandidate> result = adapter.adapt(fetchFromApi(title, author, pageSize));
            if (!result.isEmpty()) {
                return result;
            }
            log.debug("NLK 1차(제목+작가) 결과 없음. 제목만으로 재시도.");
        }
        // 2차: 제목만
        return adapter.adapt(fetchFromApi(title, null, pageSize));
    }

    // SEOJI page_size 정규화 (1~50)
    private int normalizeLimit(int limit) {
        return Math.min(Math.max(limit, 1), 50);
    }

    // SEOJI는 문자열 매칭이라 질의에 부가 표기가 붙으면 0건이 되기 쉽다.
    // 괄호/대괄호 안 텍스트만 가볍게 제거한다. (콜론 뒤 부제는 유지 — 과한 정규화는 노이즈 증가)
    private String normalizeTitleForQuery(String rawTitle) {
        if (rawTitle == null) return "";
        String t = rawTitle.replaceAll("[\\(\\[][^\\)\\]]*[\\)\\]]", " ")
                .replaceAll("\\s+", " ")
                .trim();
        return t.isEmpty() ? rawTitle.trim() : t;
    }

    // 작가는 첫 번째 인명만 사용하고 역할어(지음/저 등)·괄호 표기를 제거해 부분일치 확률을 높인다.
    private String normalizeAuthorForQuery(String rawAuthor) {
        if (rawAuthor == null || rawAuthor.isBlank()) return null;
        String a = rawAuthor.split("[,/·&;]")[0]
                .replaceAll("[\\(\\[][^\\)\\]]*[\\)\\]]", " ")
                .replaceAll("\\s*(지음|옮김|엮음|그림|감수|글|외|저|역|편)\\s*$", "")
                .replaceAll("\\s+", " ")
                .trim();
        return a.isEmpty() ? null : a;
    }

    // 국립중앙도서관 SEOJI(ISBN 서지정보) API 호출. author는 null이면 생략.
    private NlkBookAdapter.NlkResponse fetchFromApi(String title, String author, int pageSize) {
        return restClient.get()
                .uri(uri -> {
                    uri.queryParam("cert_key", certKey)
                            .queryParam("result_style", "json")
                            .queryParam("page_no", 1)
                            .queryParam("page_size", pageSize)
                            .queryParam("title", title);
                    if (author != null) {
                        uri.queryParam("author", author);
                    }
                    return uri.build();
                })
                .retrieve()
                .onStatus(HttpStatusCode::isError, (req, res) -> {
                    String body = new String(res.getBody().readAllBytes(), StandardCharsets.UTF_8);
                    throw new RuntimeException("NLK API error: " + res.getStatusCode() + " - " + body);
                })
                .body(NlkBookAdapter.NlkResponse.class);
    }
}
