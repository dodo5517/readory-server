package me.dodo.readingnotes.external.adapter;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
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
public class NlkBookAdapter implements BookApiAdapter<NlkBookAdapter.NlkResponse> {
    private static final Logger log = LoggerFactory.getLogger(NlkBookAdapter.class);
    private static final DateTimeFormatter NLK_DATE_FORMAT = DateTimeFormatter.ofPattern("yyyyMMdd");

    @Override
    public List<BookCandidate> adapt(NlkResponse response) {
        if (response == null || response.docs == null) {
            return List.of();
        }

        return response.docs.stream()
                .map(this::toCandidateSafe)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    @Override
    public String getSource() {
        return "NLK";
    }

    private BookCandidate toCandidateSafe(Doc doc) {
        if (doc == null) return null;

        String title = trimToNull(doc.title);
        if (title == null) {
            log.debug("nlk book api: title is null/blank. raw={}", doc);
            return null; // 제목 없으면 후보 제외
        }

        BookCandidate c = new BookCandidate();
        c.setSource(getSource());
        c.setTitle(title);
        c.setAuthor(trimToNull(doc.author));

        // ISBN: SEOJI는 EA_ISBN(ISBN13)만 제공. ISBN10은 없음.
        IsbnPair pair = parseIsbnPair(defaultStr(doc.eaIsbn) + " " + defaultStr(doc.setIsbn));
        c.setIsbn10(defaultStr(pair.isbn10));
        c.setIsbn13(defaultStr(pair.isbn13));

        c.setPublisher(defaultStr(doc.publisher));
        c.setPublishedDate(parseDate(doc.publishPredate));
        c.setThumbnailUrl(defaultStr(doc.titleUrl));

        // externalId: 제어번호 우선, 없으면 ISBN13
        String controlNo = trimToNull(doc.controlNo);
        c.setExternalId(defaultStr(controlNo != null ? controlNo : pair.isbn13));

        c.setScore(0.0);

        log.debug("nlkBook BookCandidate: {}", c);
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

    // SEOJI 날짜 형식 파싱 (yyyyMMdd)
    private LocalDate parseDate(String dateStr) {
        try {
            if (dateStr == null) return null;
            String cleaned = dateStr.replaceAll("[^0-9]", "");
            if (cleaned.length() != 8) return null;
            return LocalDate.parse(cleaned, NLK_DATE_FORMAT);
        } catch (Exception e) {
            log.debug("Failed to parse nlk date: {}", dateStr, e);
            return null;
        }
    }

    // ===== DTO =====

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class NlkResponse {
        @JsonProperty("TOTAL_COUNT")
        public String totalCount;
        @JsonProperty("PAGE_NO")
        public String pageNo;
        public List<Doc> docs;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Doc {
        @JsonProperty("TITLE")
        public String title;
        @JsonProperty("AUTHOR")
        public String author;
        @JsonProperty("PUBLISHER")
        public String publisher;
        @JsonProperty("EA_ISBN")
        public String eaIsbn;      // ISBN13
        @JsonProperty("SET_ISBN")
        public String setIsbn;
        @JsonProperty("PUBLISH_PREDATE")
        public String publishPredate;  // yyyyMMdd
        @JsonProperty("TITLE_URL")
        public String titleUrl;    // 표지 이미지 URL
        @JsonProperty("CONTROL_NO")
        public String controlNo;   // 제어번호

        @Override
        public String toString() {
            return "Doc{title='" + title + "', author='" + author + "', eaIsbn='" + eaIsbn + "'}";
        }
    }
}
