package me.dodo.readingnotes.scheduler;

import me.dodo.readingnotes.domain.Book;
import me.dodo.readingnotes.dto.book.BookCandidate;
import me.dodo.readingnotes.external.client.KakaoBookClient;
import me.dodo.readingnotes.repository.BookRepository;
import me.dodo.readingnotes.service.BookRefreshService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 캐시된 도서 정보를 최신 상태로 유지하기 위한 배치.
 * 카카오 개발자 운영정책상 캐시된 데이터는 최신 상태로 유지해야 하므로,
 * 최종 갱신 후 STALE_DAYS 이상 지난 도서를 매일 일정량 재조회한다.
 */
@Component
public class BookRefreshScheduler {

    private static final Logger log = LoggerFactory.getLogger(BookRefreshScheduler.class);

    // 이 기간(일) 이상 지난 도서를 재조회 대상으로 본다.
    private static final long STALE_DAYS = 90;
    // 한 번 실행당 재조회 상한 (일일 API 한도 보호)
    private static final int MAX_PER_RUN = 100;

    private final BookRepository bookRepository;
    private final KakaoBookClient kakaoBookClient;
    private final BookRefreshService bookRefreshService;

    public BookRefreshScheduler(BookRepository bookRepository,
                                KakaoBookClient kakaoBookClient,
                                BookRefreshService bookRefreshService) {
        this.bookRepository = bookRepository;
        this.kakaoBookClient = kakaoBookClient;
        this.bookRefreshService = bookRefreshService;
    }

    // 매일 오전 4시 실행 (공지 만료 배치와 겹치지 않게)
    @Scheduled(cron = "0 0 4 * * *")
    public void refreshStaleBooks() {
        LocalDateTime threshold = LocalDateTime.now().minusDays(STALE_DAYS);
        List<Book> staleBooks = bookRepository.findStaleBooks(threshold, PageRequest.of(0, MAX_PER_RUN));

        if (staleBooks.isEmpty()) {
            log.info("도서 캐시 갱신: 대상 없음");
            return;
        }

        int refreshed = 0, notFound = 0, failed = 0;
        for (Book book : staleBooks) {
            try {
                // 네트워크 호출은 트랜잭션 밖에서 수행
                List<BookCandidate> results = kakaoBookClient.searchByIsbn(book.getIsbn13());
                BookCandidate fresh = (results == null || results.isEmpty()) ? null : results.get(0);

                if (fresh == null) {
                    notFound++;
                    continue; // 결과 없으면 갱신하지 않고 다음 실행에 재시도
                }
                // 반영은 도서 단위 짧은 트랜잭션으로
                bookRefreshService.applyRefresh(book.getId(), fresh);
                refreshed++;
            } catch (Exception e) {
                failed++;
                log.warn("도서 캐시 갱신 실패: bookId={}, isbn13={}", book.getId(), book.getIsbn13(), e);
            }
        }

        log.info("도서 캐시 갱신 완료: 대상 {}건, 갱신 {}건, 결과없음 {}건, 실패 {}건",
                staleBooks.size(), refreshed, notFound, failed);
    }
}
