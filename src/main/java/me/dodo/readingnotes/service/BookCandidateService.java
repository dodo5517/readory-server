package me.dodo.readingnotes.service;

import me.dodo.readingnotes.domain.Book;
import me.dodo.readingnotes.dto.book.BookCandidate;
import me.dodo.readingnotes.external.BookSearchClient;
import org.apache.commons.text.similarity.LevenshteinDistance;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class BookCandidateService {
    private static final Logger log = LoggerFactory.getLogger(BookCandidateService.class);

    private final List<BookSearchClient> clients;
    private final BookMatcherService bookMatcherService;

    @Autowired
    public BookCandidateService(List<BookSearchClient> clients,
                                BookMatcherService bookMatcherService) {
        this.clients = clients;
        this.bookMatcherService = bookMatcherService;
    }

    // Book Table에서 검색
    public List<BookCandidate> findCandidatesLocal(String rawTitle, String rawAuthor, int limit) {
        List<Book> existingBooks;
        try {
            existingBooks = bookMatcherService.fetchCandidatesFromBookTable(rawTitle, rawAuthor, limit);
            // 결과 있으면 Book -> BookCandidate로 변환
            if (existingBooks != null && !existingBooks.isEmpty()){
                List<BookCandidate> candidates = existingBooks.stream()
                        .map(b -> {
                            BookCandidate c = new BookCandidate();
                            c.setSource("LOCAL"); // local에서 찾은 책이라면 book table에 upsert 안 함.
                            c.setExternalId(String.valueOf(b.getId())); // externalId가 String이므로
                            c.setTitle(b.getTitle());
                            c.setAuthor(b.getAuthor());
                            c.setIsbn10(b.getIsbn10());
                            c.setIsbn13(b.getIsbn13());
                            c.setPublisher(b.getPublisher());
                            c.setPublishedDate(b.getPublishedDate());
                            c.setThumbnailUrl(b.getCoverUrl());
                            c.setScore(0.0);
                            return c;
                        })
                        .collect(Collectors.toList());
                return candidates;
            }
            // 결과 없으면 외부 API 호출
            log.debug("local에 후보 없음. 외부 api 호출.");
            return findCandidatesExternal(rawTitle, rawAuthor, limit);
        } catch (Exception e) {
            log.error("local에 후보 가져오다가 에러남: ", e);
            // 예외 발생 시에도 외부 API 호출
            return findCandidatesExternal(rawTitle, rawAuthor, limit);
        }
    }

    // 외부 api로 검색
    // clients는 [KakaoBookClient, NaverBookClient] 순서임.
    public List<BookCandidate> findCandidatesExternal(String rawTitle, String rawAuthor, int limit) {
        for (BookSearchClient client : clients) {
            try {
                List<BookCandidate> result =
                        client.search(rawTitle, rawAuthor, limit);

                // 결과가 있으면 점수 계산 후 반환
                if (result != null && !result.isEmpty()) {
                    // 점수화
                    for (BookCandidate c : result) {
                        double t = similarity(
                                norm(rawTitle),
                                norm(c.getTitle())
                        );
                        double a = similarity(
                                norm(rawAuthor),
                                norm(c.getAuthor())
                        );
                        c.setScore(0.7 * t + 0.3 * a);
                    }
                    log.debug(
                            "Selected client: {}, result: {}",
                            client.getClass().getSimpleName(),
                            result
                    );
                    // 점수가 높은 순으로 정렬 + limit 적용 후 반환
                    return result.stream()
                            .sorted(
                                    Comparator.comparingDouble(
                                            BookCandidate::getScore
                                    ).reversed()
                            )
                            .limit(Math.min(limit, 20))
                            .collect(Collectors.toList());
                }
            } catch (Exception e) {
                log.warn(
                        "Client failed: {}",
                        client.getClass().getSimpleName(),
                        e
                );
            }
        }
        // 모든 client 실패 시
        return Collections.emptyList();
    }

    // 소문자화, 문자부호/공백 제거
    private String norm(String s) {
        if (s == null) return "";
        return s.toLowerCase().replaceAll("[\\p{Punct}\\s]+", "");
    }

    // 레벤슈타인 거리 알고리즘 사용
    private double similarity(String a, String b) {
        if (a.isEmpty() || b.isEmpty()) return 0d;
        int dist = LevenshteinDistance.getDefaultInstance().apply(a, b);
        int max = Math.max(a.length(), b.length());
        return 1.0 - ((double) dist / (double) max);
    }

    // local에서 찾은 책 후보로 변환
    private BookCandidate toCandidateSafe(Book book) {
        if (book == null) return null;

        BookCandidate c = new BookCandidate();
        c.setSource("LOCAL");

        c.setTitle(trimToNull(book.getTitle()));
        c.setAuthor(trimToNull(book.getAuthor()));
        c.setPublisher(trimToNull(book.getPublisher()));

        // ISBN
        c.setIsbn10(trimToNull(book.getIsbn10()));
        c.setIsbn13(trimToNull(book.getIsbn13()));

        // externalId는 내부 book id 사용 (또는 isbn13)
        if (book.getIsbn13() != null) {
            c.setExternalId(book.getIsbn13());
        } else if (book.getId() != null) {
            c.setExternalId(String.valueOf(book.getId()));
        }

        // 날짜
        c.setPublishedDate(book.getPublishedDate());

        // 썸네일
        c.setThumbnailUrl(trimToNull(book.getCoverUrl()));

        // 점수는 서비스에서 계산
        c.setScore(0.0);

        log.debug("Local BookCandidate: {}", c);

        return c;
    }

    private static String trimToNull(String s) {
        if (s == null) return null;
        String t = s.trim();
        return t.isEmpty() ? null : t;
    }
}
