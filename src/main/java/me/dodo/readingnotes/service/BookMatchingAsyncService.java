package me.dodo.readingnotes.service;

import jakarta.transaction.Transactional;
import me.dodo.readingnotes.domain.Book;
import me.dodo.readingnotes.domain.ReadingRecord;
import me.dodo.readingnotes.dto.book.BookCandidate;
import me.dodo.readingnotes.dto.book.LinkBookRequest;
import me.dodo.readingnotes.dto.book.MatchResult;
import me.dodo.readingnotes.external.client.KakaoBookClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class BookMatchingAsyncService {
    private static final Logger log = LoggerFactory.getLogger(BookMatchingAsyncService.class);

    private final BookMatcherService bookMatcherService;
    private final KakaoBookClient kakaoBookClient;
    private final BookLinkService bookLinkService;

    @Autowired
    public BookMatchingAsyncService(BookMatcherService bookMatcherService,
                                    KakaoBookClient kakaoBookClient, BookLinkService bookLinkService){
        this.bookMatcherService = bookMatcherService;
        this.kakaoBookClient = kakaoBookClient;
        this.bookLinkService = bookLinkService;
    }

    // 책 검색 후 매칭
    @Async
    @Transactional
    public void matchAndSave(ReadingRecord record) {
        // 기존 책 테이블에서 책 검색 (최대 10개)
        List<Book> existingBooks = bookMatcherService.fetchCandidatesFromBookTable(
                record.getRawTitle(), record.getRawAuthor(), 10);
        // 최종 선택된 책
        MatchResult result;

        // 책 테이블에 책이 있다면
        if (!existingBooks.isEmpty()) {
            // Book -> BookCandidate 변환
            List<BookCandidate> candidates = existingBooks.stream()
                    .map(b -> toBookCandidate(b))
                    .collect(Collectors.toList());
            // 매칭 시도
            result = bookMatcherService.pickBest(record.getRawTitle(), record.getRawAuthor(), candidates);

            // 강매칭이거나 높은 점수면 기존 책 사용
            if (result.isAutoMatch() || result.getScore() > 0.85) {
                log.info("기존 책 테이블에서 매칭 성공: {} (score: {})", result.getBest().getTitle(), result.getScore());
                saveMatchResult(record, result);
                return;
            }

            log.info("기존 책 중 확실한 매칭 없음. 외부 API 검색 진행...");
        }
        // Kakao 검색
        List<BookCandidate> candidates = kakaoBookClient.search(record.getRawTitle(), record.getRawAuthor(), 10);
        // BookMatcher로 베스트 선택
        result = bookMatcherService.pickBest(record.getRawTitle(), record.getRawAuthor(), candidates);
        saveMatchResult(record, result); // 매칭 저장
    }

    private void saveMatchResult(ReadingRecord record, MatchResult result) {
        if (result.getBest() == null || !result.isAutoMatch()) return;
        //검색결과 DTO → 저장 명령 DTO 변환
        LinkBookRequest reqDto = LinkBookRequest.fromCandidate(result.getBest());
        // 스냅샷(근거 데이터) JSON 구성
        Map<String, Object> snapshot = Map.of(
                "provider", reqDto.getSource(),
                "score", result.getScore(),
                "query", Map.of("title", record.getRawTitle(), "author", record.getRawAuthor()),
                "candidate", Map.of(
                        "title", result.getBest().getTitle(),
                        "author", result.getBest().getAuthor(),
                        "isbn10", result.getBest().getIsbn10(),
                        "isbn13", result.getBest().getIsbn13(),
                        "publisher", result.getBest().getPublisher(),
                        "publishedDate", result.getBest().getPublishedDate() == null ? null : result.getBest().getPublishedDate().toString(),
                        "thumbnailUrl", result.getBest().getThumbnailUrl(),
                        "externalId", result.getBest().getExternalId()
                ),
                "matcher", Map.of(
                        "threshold", 0.88,
                        "weights", Map.of("title", 0.7, "author", 0.3),
                        "version", "2025-08-18"
                )
        );

        bookLinkService.linkRecordAuto(record.getId(), reqDto, result.getScore(), toJsonSafe(snapshot));
    }
    // 간단 버전: 필요시 Jackson 빈 주입으로 교체(아래 참고)
    private String toJsonSafe(Object obj) {
        try {
            return new com.fasterxml.jackson.databind.ObjectMapper().writeValueAsString(obj);
        } catch (Exception e) {
            return null;
        }
    }

    private BookCandidate toBookCandidate(Book b) {
        // Book -> BookCandidate 변환 코드 그대로
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
    }
}