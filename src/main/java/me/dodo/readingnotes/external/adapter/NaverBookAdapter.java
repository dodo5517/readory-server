package me.dodo.readingnotes.external.adapter;

import me.dodo.readingnotes.dto.book.BookCandidate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Component
public class NaverBookAdapter implements BookApiAdapter<NaverBookAdapter.NaverResponse> {
    private static final Logger log = LoggerFactory.getLogger(NaverBookAdapter.class);
    private static final DateTimeFormatter NAVER_DATE_FORMAT = DateTimeFormatter.ofPattern("yyyyMMdd");

    @Override
    public List<BookCandidate> adapt(NaverResponse response) {
        if (response == null || response.items == null) {
            return List.of();
        }

        return response.items.stream()
                .map(this::toCandidateSafe)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    @Override
    public String getSource() {
        return "NAVER";
    }

    private BookCandidate toCandidateSafe(Item item) {
        if (item == null) return null;

        String title = removeHtmlTags(item.title);
        if (title == null || title.isBlank()) {
            log.debug("naver book api: title is null/blank. raw={}", item);
            return null;
        }

        BookCandidate c = new BookCandidate();
        c.setSource(getSource());
        c.setTitle(title);

        // author - HTML 태그 제거
        String author = removeHtmlTags(item.author);
        c.setAuthor(trimToNull(author));

        // externalId: link 사용
        c.setExternalId(defaultStr(item.link));

        // ISBN 정제
        IsbnPair pair = parseIsbnPair(item.isbn);
        c.setIsbn10(defaultStr(pair.isbn10));
        c.setIsbn13(defaultStr(pair.isbn13));

        // publisher - HTML 태그 제거
        String publisher = removeHtmlTags(item.publisher);
        c.setPublisher(defaultStr(publisher));

        c.setPublishedDate(parseDate(item.pubdate));
        c.setThumbnailUrl(defaultStr(item.image));
        c.setScore(0.0);

        log.debug("naverBook BookCandidate: {}", c);
        return c;
    }

    // ===== Util =====

    private static class IsbnPair {
        final String isbn10;
        final String isbn13;
        IsbnPair(String i10, String i13) { this.isbn10 = i10; this.isbn13 = i13; }
    }

    private IsbnPair parseIsbnPair(String raw) {
        if (raw == null) return new IsbnPair(null, null);

        String[] tokens = raw.trim().split("\\s+");
        String i10 = null, i13 = null;

        for (String tk : tokens) {
            if (tk == null) continue;
            String cleaned = tk.replaceAll("[^0-9Xx]", "").toUpperCase();
            if (cleaned.length() == 10 && i10 == null) i10 = cleaned;
            else if (cleaned.length() == 13 && i13 == null) i13 = cleaned;
        }
        return new IsbnPair(i10, i13);
    }

    // HTML 태그 제거 (<b>, </b> 등)
    private String removeHtmlTags(String text) {
        if (text == null) return null;
        return text.replaceAll("<[^>]*>", "");
    }

    private static String defaultStr(String s) {
        return s == null ? "" : s;
    }

    private static String trimToNull(String s) {
        if (s == null) return null;
        String t = s.trim();
        return t.isEmpty() ? null : t;
    }

    // Naver 날짜 형식 파싱 (yyyyMMdd)
    private LocalDate parseDate(String dateStr) {
        try {
            if (dateStr == null || dateStr.isBlank()) return null;
            return LocalDate.parse(dateStr, NAVER_DATE_FORMAT);
        } catch (Exception e) {
            log.debug("Failed to parse naver date: {}", dateStr, e);
            return null;
        }
    }

    // ===== DTO =====

    public static class NaverResponse {
        public String lastBuildDate;
        public int total;
        public int start;
        public int display;
        public List<Item> items;
    }

    public static class Item {
        public String title;          // <b> 태그 포함 가능
        public String link;
        public String image;
        public String author;         // HTML 태그 포함 가능
        public String discount;       // 할인가
        public String publisher;      // HTML 태그 포함 가능
        public String pubdate;        // yyyyMMdd 형식
        public String isbn;           // ISBN10 ISBN13 공백으로 구분
        public String description;

        @Override
        public String toString() {
            return "Item{title='" + title + "', author='" + author + "', isbn='" + isbn + "'}";
        }
    }
}
