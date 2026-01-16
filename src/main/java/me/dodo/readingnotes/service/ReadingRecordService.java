package me.dodo.readingnotes.service;

import jakarta.persistence.EntityNotFoundException;
import me.dodo.readingnotes.config.OAuth2SuccessHandler;
import me.dodo.readingnotes.domain.Book;
import me.dodo.readingnotes.domain.ReadingRecord;
import me.dodo.readingnotes.domain.User;
import me.dodo.readingnotes.dto.book.*;
import me.dodo.readingnotes.dto.reading.ReadingRecordItem;
import me.dodo.readingnotes.dto.reading.ReadingRecordRequest;
import me.dodo.readingnotes.dto.reading.ReadingRecordResponse;
import me.dodo.readingnotes.external.KakaoBookClient;
import me.dodo.readingnotes.repository.BookRepository;
import me.dodo.readingnotes.repository.ReadingRecordRepository;
import me.dodo.readingnotes.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static me.dodo.readingnotes.domain.ReadingRecord.MatchStatus.PENDING;

@Service
public class ReadingRecordService {

    private final ReadingRecordRepository readingRecordRepository;
    private final UserRepository userRepository;
    private final BookRepository bookRepository;
    private final KakaoBookClient kakaoBookClient;
    private final BookMatcherService bookMatcherService;
    private final BookLinkService bookLinkService;

    private static final Logger log = LoggerFactory.getLogger(ReadingRecordService.class);

    private static final int MAX_PAGE_SIZE = 30;
    private static final ZoneId ZONE = ZoneId.of("Asia/Seoul");

    @Autowired
    public ReadingRecordService(ReadingRecordRepository readingRecordRepository,
                                BookRepository bookRepository,
                                UserRepository userRepository, KakaoBookClient kakaoBookClient,
                                BookMatcherService bookMatcherService, BookLinkService bookLinkService) {
        this.readingRecordRepository = readingRecordRepository;
        this.userRepository = userRepository;
        this.bookRepository = bookRepository;
        this.kakaoBookClient = kakaoBookClient;
        this.bookMatcherService = bookMatcherService;
        this.bookLinkService = bookLinkService;
    }

    // 새로운 기록 생성
    @Transactional
    public ReadingRecord createByUserId(Long userId, ReadingRecordRequest req) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 사용자"));

        ReadingRecord record = new ReadingRecord();
        record.setUser(user);
        record.setSentence(req.getSentence());
        record.setComment(req.getComment());
        record.setRawTitle(req.getRawTitle());
        record.setRawAuthor(req.getRawAuthor());
        record.setRecordedAt(LocalDateTime.now());
        record.setUpdatedAt(LocalDateTime.now());

        ReadingRecord saved = readingRecordRepository.save(record);


        // 제목+작가 모두 있을 경우
        if (present(saved.getRawTitle()) && present(saved.getRawAuthor())) {
            // 책 검색 후 매칭
            matchingBook(saved);
        }
        return saved;
    }
    // 책 검색 후 매칭
    private void matchingBook(ReadingRecord record) {
        // Kakao 검색
        List<BookCandidate> candidates = kakaoBookClient.search(record.getRawTitle(), record.getRawAuthor(), 10);

        // BookMatcher로 베스트 선택
        BookMatcherService.MatchResult result =
                bookMatcherService.pickBest(record.getRawTitle(), record.getRawAuthor(), candidates);

        // 자동 확정이면 저장 진입점으로 위임
        if (result.best != null && result.autoMatch) {
            //검색결과 DTO → 저장 명령 DTO 변환
            LinkBookRequest reqDto = LinkBookRequest.fromCandidate(result.best);

            // 스냅샷(근거 데이터) JSON 구성
            Map<String, Object> snapshot = Map.of(
                    "provider", reqDto.getSource(), // "KAKAO"
                    "score", result.score,
                    "query", Map.of("title", record.getRawTitle(), "author", record.getRawAuthor()),
                    "candidate", Map.of(
                            "title", result.best.getTitle(),
                            "author", result.best.getAuthor(),
                            "isbn10", result.best.getIsbn10(),
                            "isbn13", result.best.getIsbn13(),
                            "publisher", result.best.getPublisher(),
                            "publishedDate", result.best.getPublishedDate() == null ? null : result.best.getPublishedDate().toString(),
                            "thumbnailUrl", result.best.getThumbnailUrl(),
                            "externalId", result.best.getExternalId()
                    ),
                    "matcher", Map.of(
                            "threshold", 0.88,
                            "weights", Map.of("title", 0.7, "author", 0.3),
                            "version", "2025-08-18"
                    )
            );
            String snapshotJson = toJsonSafe(snapshot); // 아래 유틸 참고

            // Book/Link/Record 한 번에 처리
            bookLinkService.linkRecordAuto(record.getId(), reqDto, result.score, snapshotJson);
        }
    }
    private boolean present(String s) { return s != null && !s.isBlank(); }
    // 간단 버전: 필요시 Jackson 빈 주입으로 교체(아래 참고)
    private String toJsonSafe(Object obj) {
        try {
            return new com.fasterxml.jackson.databind.ObjectMapper().writeValueAsString(obj);
        } catch (Exception e) {
            return null;
        }
    }

    // 해당 유저의 최신 N개 기록 조회
    public List<ReadingRecord> getLatestRecords(Long userId, int size) {
        PageRequest pr = PageRequest.of(0, size, Sort.by(Sort.Direction.DESC, "recordedAt"));
        return readingRecordRepository.findLatestByUser(userId, pr);
    }

    // 해당 유저의 모든 기록 조회(제목/작가 or 문장/코멘트 로 검색)
    public Page<ReadingRecord> getMyRecords(Long userId, String scope, String q, Pageable pageable) {
        // q가 비어있으면 null로 전달 → 쿼리에서 전체 조회 + 최신순 정렬(Pageable)
        String normalizedQ = (q != null && !q.trim().isEmpty()) ? q.trim() : null;
        if ("titleAndAuthor".equalsIgnoreCase(scope)) {
            return readingRecordRepository.findMyRecordsByBook(userId, normalizedQ, pageable);
        }
        return readingRecordRepository.findMyRecordsByText(userId, normalizedQ, pageable);
    }

    // 해당 유저의 매칭 끝난 책 리스트 조회
    @Transactional(readOnly = true)
    public Page<BookWithLastRecordResponse> getConfirmedBooks(Long userId, String q, Pageable pageable, String sort) {
        if ("title".equalsIgnoreCase(sort)) {
            return readingRecordRepository.findConfirmedBooksByTitle(userId, q, pageable);
        }
        return readingRecordRepository.findConfirmedBooksByRecent(userId, q, pageable);
    }

    // 해당 유저가 기록한 책 한 권에 대한 기록 조회
    @Transactional(readOnly = true)
    public BookRecordsPageResponse getBookRecordsByCursor(Long userId, Long bookId, String cursor, int size) {
        // 책 찾기
        Book book = bookRepository.findById(bookId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 책입니다."));

        // size 정규화
        int pageSize = normalizeSize(size);
        Cursor c = parseCursor(cursor);

        // 기록 시간 내림차순 → id 내림차순.
        Sort sort = Sort.by("recordedAt").descending().and(Sort.by("id").descending());
        // 커서가 있다면 커서보다 더 작은(과거) 레코드만 가져옴.
        // sqlite
//        List<ReadingRecord> fetched = readingRecordRepository.findSliceByUserAndBookWithCursor(
//                userId, bookId, c.cursorAt, c.cursorId, PageRequest.of(0, pageSize + 1, sort)
//        );
        // postgreSql
        List<ReadingRecord> fetched;
        if (c == null || c.cursorAt == null || c.cursorId == null) {
            // 첫 페이지
            fetched = readingRecordRepository.findSliceFirstPage(
                    userId, bookId, PageRequest.of(0, pageSize + 1, sort)
            );
        } else {
            // 다음 페이지
            fetched = readingRecordRepository.findSliceNextPage(
                    userId, bookId, c.cursorAt, c.cursorId, PageRequest.of(0, pageSize + 1, sort)
            );
        }

        // 기록이 더 남았는지 확인(남았으면=true, 안 남았으면=false)
        boolean hasMore = fetched.size() > pageSize;
        // 더 남았어도 pageSize만큼만 가져옴
        if (hasMore) fetched = new ArrayList<>(fetched.subList(0, pageSize));

        // 현재 페이지의 마지막 요소의 (recordedAt, id)를 커서 문자열(“epochMillis_id”)로 직렬화하여 반환
        String nextCursor = null;
        if (hasMore && !fetched.isEmpty()) {
            ReadingRecord last = fetched.get(fetched.size() - 1);
            nextCursor = buildCursor(last.getRecordedAt(), last.getId());
        }

        // 기간 계산
        LocalDateTime minAt = readingRecordRepository.findMinRecordedAtByUserAndBook(userId, bookId);
        LocalDateTime maxAt = readingRecordRepository.findMaxRecordedAtByUserAndBook(userId, bookId);

        String periodStart = (minAt == null) ? null : minAt.toString();
        String periodEnd   = (maxAt == null) ? null : maxAt.toString();

        // 책 정보 구성
        BookMetaResponse bookMeta = new BookMetaResponse(
                book.getId(),
                book.getTitle(),
                book.getAuthor(),
                book.getPublisher(),
                book.getPublishedDate() != null ? book.getPublishedDate().toString() : null,
                book.getCoverUrl(),
                periodStart,
                periodEnd
        );

        // 기록 정보 매핑
        List<ReadingRecordItem> items = fetched.stream()
                .map(r -> new ReadingRecordItem(r.getId(), r.getRecordedAt(), r.getSentence(), r.getComment()))
                .toList();

        return new BookRecordsPageResponse(bookMeta, items, nextCursor, hasMore);
    }
    // pageSize 최소/최대 규정
    private int normalizeSize(int size) {
        if (size <= 0) return 20;
        return Math.min(size, MAX_PAGE_SIZE);
    }
    // cursorAt,id를 저장
    private static class Cursor {
        final LocalDateTime cursorAt;
        final Long cursorId;
        Cursor(LocalDateTime at, Long id) { this.cursorAt = at; this.cursorId = id; }
    }
    // "epochMillis_id" -> (LocalDateTime, id)로 변환
    // null이면 첫 페이지라는 뜻임.
    private Cursor parseCursor(String cursor) {
        if (cursor == null || cursor.isBlank()) return new Cursor(null, null);
        String[] parts = cursor.split("_");
        if (parts.length != 2) throw new IllegalArgumentException("커서 형식이 올바르지 않습니다.");
        long epochMillis = Long.parseLong(parts[0]);
        long id = Long.parseLong(parts[1]);
        LocalDateTime at = LocalDateTime.ofInstant(Instant.ofEpochMilli(epochMillis), ZONE);
        return new Cursor(at, id);
    }
    // (recordedAt, id) -> "epochMillis_id"로 직렬화
    private String buildCursor(LocalDateTime recordedAt, Long id) {
        long epochMillis = recordedAt.atZone(ZONE).toInstant().toEpochMilli();
        return epochMillis + "_" + id;
    }

    // 기록 수정
    public ReadingRecordResponse update(Long recordId, Long userId, ReadingRecordRequest request) {
        Optional<ReadingRecord> recordOpt = readingRecordRepository.findByIdAndUserId(recordId, userId);
        if (recordOpt.isEmpty()) {
            log.info("해당 유저의 해당 기록이 없습니다: {} 의 {}", userId, recordId);
            throw new EntityNotFoundException("해당 기록을 찾을 수 없습니다.");
        }
        // 기존 기록
        ReadingRecord record = recordOpt.get();
        log.debug("requet record: {}", request.toString());

        // null을 제외한 빈 문자열("")은 덮어쓰기함.
        Optional.ofNullable(request.getRawTitle()).ifPresent(record::setRawTitle);
        Optional.ofNullable(request.getRawAuthor()).ifPresent(record::setRawAuthor);
        Optional.ofNullable(request.getSentence()).ifPresent(record::setSentence);
        Optional.ofNullable(request.getComment()).ifPresent(record::setComment);
        record.setUpdatedAt(LocalDateTime.now());

        // 책 정보 변경 시
        if (request.getRawTitle() != null || request.getRawAuthor() != null) {
            // 기존 책 연결 끊기
            bookLinkService.removeBookMatch(recordId);

            // 제목+작가 모두 있을 경우 책 정보 재매칭
            if (present(record.getRawTitle()) && present(record.getRawAuthor())) {
                matchingBook(record);
            }
        }
        log.debug("saved record: {}", request.toString());
        // 수정한 기록 저장
        ReadingRecord saved = readingRecordRepository.save(record);

        // DTO로 변환
        return ReadingRecordResponse.fromEntity(saved);
    }

    // 기록 삭제
    public void deleteRecordById(Long recordId, Long userId) {
        // 삭제하려는 행의 존재 여부 확인
        ReadingRecord record = readingRecordRepository.findByIdAndUserId(recordId, userId)
                .orElseThrow(()-> new IllegalArgumentException("해당 유저의 해당 레코드가 존재하지 않습니다: "+ userId +"의"+ recordId));
        // 삭제
        readingRecordRepository.delete(record);
    }

}
