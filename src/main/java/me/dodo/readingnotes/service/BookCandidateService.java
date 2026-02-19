package me.dodo.readingnotes.service;

import me.dodo.readingnotes.dto.book.BookCandidate;
import me.dodo.readingnotes.external.BookSearchClient;
import org.apache.commons.text.similarity.LevenshteinDistance;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class BookCandidateService {
    private static final Logger log = LoggerFactory.getLogger(BookCandidateService.class);

    private final List<BookSearchClient> clients;

    public BookCandidateService(List<BookSearchClient> clients) {
        this.clients = clients;
    }

    // clients는 [KakaoBookClient, NaverBookClient] 순서임.
    public List<BookCandidate> findCandidates(String rawTitle, String rawAuthor, int limit) {
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
}
