package me.dodo.readingnotes.service;

import me.dodo.readingnotes.domain.Book;
import me.dodo.readingnotes.dto.book.BookCandidate;
import me.dodo.readingnotes.repository.BookRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class BookRefreshService {
    private static final Logger log = LoggerFactory.getLogger(BookRefreshService.class);

    private final BookRepository bookRepo;

    public BookRefreshService(BookRepository bookRepo) {
        this.bookRepo = bookRepo;
    }

    // 외부 API로 재조회한 최신 데이터를 도서에 반영 (도서 단위 트랜잭션)
    // fresh가 null이거나 동일 도서가 아니면 갱신하지 않음(다음 실행에 재시도).
    @Transactional
    public void applyRefresh(Long bookId, BookCandidate fresh) {
        Book book = bookRepo.findById(bookId).orElse(null);
        if (book == null) return;

        // 동일 도서인지 ISBN13으로 확인. 다르면 잘못된 결과이므로 반영하지 않음.
        if (fresh == null || fresh.getIsbn13() == null
                || !fresh.getIsbn13().equals(book.getIsbn13())) {
            log.debug("도서 갱신 스킵(동일 도서 아님/결과 없음): bookId={}, isbn13={}", bookId, book.getIsbn13());
            return;
        }

        // 응답에 빈 값이 오면 기존 값을 유지(coalesce) 함.
        book.updateFrom(
                coalesce(fresh.getTitle(), book.getTitle()),
                coalesce(fresh.getAuthor(), book.getAuthor()),
                coalesce(fresh.getPublisher(), book.getPublisher()),
                coalesce(fresh.getIsbn10(), book.getIsbn10()),
                book.getIsbn13(), // 조회 키이므로 그대로 유지
                coalesce(fresh.getThumbnailUrl(), book.getCoverUrl()),
                fresh.getPublishedDate() != null ? fresh.getPublishedDate() : book.getPublishedDate()
        );
        // 값이 그대로여도 재확인했음을 기록.
        book.markRefreshed();
    }

    private static String coalesce(String fresh, String fallback) {
        return (fresh != null && !fresh.isBlank()) ? fresh : fallback;
    }
}
