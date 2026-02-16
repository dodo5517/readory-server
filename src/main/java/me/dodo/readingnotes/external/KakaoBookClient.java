package me.dodo.readingnotes.external;

import me.dodo.readingnotes.dto.book.BookCandidate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@Component
public class KakaoBookClient implements BookSearchClient {
    private static final Logger log = LoggerFactory.getLogger(KakaoBookClient.class);
    private final WebClient webClient;

    public KakaoBookClient(@Qualifier("kakaoBookWebClient") WebClient webClient) {
        this.webClient = webClient;
    }

    @Override
    public List<BookCandidate> search(String rawTitle, String rawAuthor, int limit) {
        String query = (rawAuthor == null || rawAuthor.isBlank())
                ? "\"" + rawTitle + "\""
                : "\"" + rawTitle + "\" \"" + rawAuthor + "\"";

        // 카카오 API size 허용 범위를 1~50개로.
        int size = Math.min(Math.max(limit, 1), 50);

        // 책 검색
        KakaoResponse resp = webClient.get()
                .uri(uri -> uri.queryParam("query", query)
                        .queryParam("size", size)
                        .queryParam("sort", "accuracy") // 정확도순으로 정렬
                        .build())
                .retrieve()
                // 4xx/5xx 응답을 에러로 변환함
                .onStatus(status -> status.isError(), r ->
                        r.bodyToMono(String.class).map(body ->
                                new RuntimeException("Kakao API error: " +  r.statusCode()  + " - " + body)))
                .bodyToMono(KakaoResponse.class)
                .block();

        if (resp == null || resp.documents == null) return List.of();

//        log.debug("Kakao API response: {}", resp.documents.toString());

        // BookCandidate로 변환
        return resp.documents.stream()
                .map(this::toCandidateSafe)   
                .filter(Objects::nonNull)     // ← 변환 실패/스킵된 문서 제거
                .limit(size)                  // 혹시 초과시 안전
                .collect(Collectors.toList());
    }

    private BookCandidate toCandidateSafe(Document d) {
        if (d == null) return null;

        String title = trimToNull(d.title);
        if (title == null) {
            log.debug("skip kakao doc: title is null/blank. raw={}", d);
            return null; // 제목 없으면 후보 제외
        }

        BookCandidate c = new BookCandidate();
        c.setSource(getSource());
        c.setTitle(title);

        // authors → "A, B" (null 안전)
        String author = joinAuthor(d.authors);
        if (author != null && author.isBlank()) author = null;
        c.setAuthor(author);

        // externalId: url 우선, 없으면 isbn 원문
        // externalId: 카카오는 별도 ID가 없어서 url 또는 isbn13 활용
        c.setExternalId(defaultStr(d.url) != null ? d.url : trimToNull(d.isbn));

        // ISBN 정제: 숫자/‘X’만 남기고 10/13 자리만 선별
        IsbnPair pair = parseIsbnPair(d.isbn);
        c.setIsbn10(defaultStr(pair.isbn10));
        c.setIsbn13(defaultStr(pair.isbn13));

        c.setPublisher(defaultStr(d.publisher));
        c.setPublishedDate(parseDate(d.datetime));
        c.setThumbnailUrl(defaultStr(d.thumbnail));
        c.setScore(0.0);

        log.debug("kakaoBook BookCandidate: {}", c);
        return c;
    }
    private static class IsbnPair {
        final String isbn10;
        final String isbn13;
        IsbnPair(String i10, String i13) { this.isbn10 = i10; this.isbn13 = i13; }
    }
    // isbn 정제
    private IsbnPair parseIsbnPair(String raw) {
        if (raw == null) return new IsbnPair(null, null);
        // 공백/쉼표로 토큰화 후 숫자/대문자X만 남김 (하이픈/문자 제거, 소문자x → X)
        String[] tokens = raw.trim().split("\\s+|,");
        String i10 = null, i13 = null;
        for (String tk : tokens) {
            if (tk == null) continue;
            String cleaned = tk.replaceAll("[^0-9Xx]", "").toUpperCase();
            if (cleaned.length() == 10 && i10 == null) i10 = cleaned;
            else if (cleaned.length() == 13 && i13 == null) i13 = cleaned;
        }
        return new IsbnPair(i10, i13);
    }
    private static String defaultStr(String s) { return s == null ? "" : s; }
    private static String trimToNull(String s) {
        if (s == null) return null;
        String t = s.trim();
        return t.isEmpty() ? null : t;
    }

    // author 리스트를 "A, B"로 합치기 (null/빈 처리 포함)
    private static String joinAuthor(List<String> author) {
        if (author == null || author.isEmpty()) return "";
        String joined = author.stream()
                .filter(Objects::nonNull)
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .collect(Collectors.joining(", "));
        return joined.isEmpty() ? null : joined;
    }

    // 날짜 파싱("2020-01-02T10:20:30.000+09:00" 같은 문자열을 LocalDate(앞 10자리)로 파싱함.)
    private LocalDate parseDate(String iso) {
        try { return (iso != null && iso.length() >= 10) ? LocalDate.parse(iso.substring(0, 10)) : null; }
        catch (Exception e) { return null; }
    }

    @Override public String getSource() { return "KAKAO"; }

    // 내부 응답 DTO
    static class KakaoResponse {
        public Meta meta;
        public List<Document> documents;
    }
    static class Meta {
        public boolean is_end;
        public int pageable_count;
        public int total_count;
    }
    static class Document {
        public String title;
        public String contents;
        public String url;
        public String isbn;
        public String datetime;
        // 카카오는 authors로 줌.
        public List<String> authors;
        public String publisher;
        public String[] translators;
        public Integer price;
        public Integer sale_price;
        public String thumbnail;
        public String status;

        @Override
        public String toString() {
            return "Document{title='" + title + "', authors=" + authors + ", isbn='" + isbn + "'}";
        }
    }
}
