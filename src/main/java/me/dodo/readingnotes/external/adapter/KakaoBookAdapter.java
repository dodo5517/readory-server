package me.dodo.readingnotes.external.adapter;

import me.dodo.readingnotes.dto.book.BookCandidate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
@Component
public class KakaoBookAdapter implements BookApiAdapter<KakaoBookAdapter.KakaoResponse>{
    private static final Logger log = LoggerFactory.getLogger(KakaoBookAdapter.class);
    @Override
    public List<BookCandidate> adapt(KakaoResponse response) {
        if (response == null || response.documents == null) {
            return List.of();
        }

        return response.documents.stream()
                .map(this::toCandidateSafe)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    @Override
    public String getSource() {
        return "KAKAO";
    }

    private BookCandidate toCandidateSafe(Document d) {
        if (d == null) return null;

        String title = trimToNull(d.title);
        if (title == null) {
            log.debug("kakao book api: title is null/blank. raw={}", d);
            return null; // 제목 없으면 후보 제외
        }

        BookCandidate c = new BookCandidate();
        c.setSource(getSource());
        c.setTitle(title);

        // 작가 이름 합치기
        String author = joinAuthor(d.authors);
        if (author != null && author.isBlank()) author = null;
        c.setAuthor(author);

        // externalId: 카카오는 별도 ID가 없어서 url 또는 isbn13 활용
        c.setExternalId(defaultStr(d.url) != null ? d.url : trimToNull(d.isbn));

        // ISBN 정제
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

    // ===== Util =====
    private static class IsbnPair {
        final String isbn10;
        final String isbn13;
        IsbnPair(String i10, String i13) { this.isbn10 = i10; this.isbn13 = i13; }
    }
    // ISBN 정제
    private IsbnPair parseIsbnPair(String raw) {
        if (raw == null) return new IsbnPair(null, null);

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

    private static String defaultStr(String s) {
        return s == null ? "" : s;
    }

    private static String trimToNull(String s) {
        if (s == null) return null;
        String t = s.trim();
        return t.isEmpty() ? null : t;
    }

    // 작가 이름 합치기
    private static String joinAuthor(List<String> authors) {
        if (authors == null) return null;
        String joined = authors.stream()
                .filter(Objects::nonNull)
                .map(String::trim) // 공백 제거
                .filter(s -> !s.isEmpty())
                .collect(Collectors.joining(", "));
        return joined.isEmpty() ? null : joined;
    }

    private LocalDate parseDate(String iso) {
        try {
            return (iso != null && iso.length() >= 10)
                    ? LocalDate.parse(iso.substring(0, 10))
                    : null;
        } catch (Exception e) {
            return null;
        }
    }

    // ===== DTO =====

    public static class KakaoResponse {
        public Meta meta;
        public List<Document> documents;
    }

    public static class Meta {
        public boolean is_end;
        public int pageable_count;
        public int total_count;
    }

    public static class Document {
        public String title;
        public String contents;
        public String url;
        public String isbn;
        public String datetime;
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
