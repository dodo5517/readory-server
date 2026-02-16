package me.dodo.readingnotes.service;

import jakarta.persistence.EntityNotFoundException;
import me.dodo.readingnotes.dto.book.MatchResult;
import me.dodo.readingnotes.domain.Book;
import me.dodo.readingnotes.domain.ReadingRecord;
import me.dodo.readingnotes.domain.User;
import me.dodo.readingnotes.dto.admin.AdminRecordDetailResponse;
import me.dodo.readingnotes.dto.admin.AdminRecordListResponse;
import me.dodo.readingnotes.dto.admin.AdminRecordUpdateRequest;
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
import java.util.stream.Collectors;

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
        // 기존 책 테이블에서 책 검색
        List<Book> existingBooks = bookMatcherService.fetchCandidatesFromBookTable(record.getRawTitle(), record.getRawAuthor());
        
        // 최종 선택된 책
        MatchResult result;

        // 책 테이블에 책이 있다면
        if (!existingBooks.isEmpty()) {
            // Book -> BookCandidate 변환
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

            // 매칭 시도
            result = bookMatcherService.pickBest(record.getRawTitle(), record.getRawAuthor(), candidates);

            // 강매칭이거나 높은 점수면 기존 책 사용
            if (result.isAutoMatch() || result.getScore() > 0.85) {
                log.info("기존 책 테이블에서 매칭 성공: {} (score: {})",
                        result.getBest().getTitle(),
                        result.getScore());
            }

            log.info("기존 책 중 확실한 매칭 없음. 외부 API 검색 진행...");
        } else {
            // Kakao 검색
            List<BookCandidate> candidates = kakaoBookClient.search(record.getRawTitle(), record.getRawAuthor(), 10);

            // BookMatcher로 베스트 선택
            result = bookMatcherService.pickBest(record.getRawTitle(), record.getRawAuthor(), candidates);
        }

        // 자동 확정이면 저장 진입점으로 위임
        if (result.getBest() != null && result.isAutoMatch()) {
            //검색결과 DTO → 저장 명령 DTO 변환
            LinkBookRequest reqDto = LinkBookRequest.fromCandidate(result.getBest());

            // 스냅샷(근거 데이터) JSON 구성
            Map<String, Object> snapshot = Map.of(
                    "provider", reqDto.getSource(), // "KAKAO"
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
            String snapshotJson = toJsonSafe(snapshot); // 아래 유틸 참고

            // Book/Link/Record 한 번에 처리
            bookLinkService.linkRecordAuto(record.getId(), reqDto, result.getScore(), snapshotJson);
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


    // ##############################
    // 관리자 전용 메서드
    // ##############################

    // 관리자용: 전체 기록 목록 조회 (검색 + 필터링)
    @Transactional(readOnly = true)
    public Page<AdminRecordListResponse> findAllRecordsForAdmin(String keyword,
                                                                ReadingRecord.MatchStatus matchStatus,
                                                                Long userId,
                                                                Pageable pageable) {
        return readingRecordRepository.findAllForAdmin(keyword, matchStatus, userId, pageable)
                .map(AdminRecordListResponse::new);
    }

    // 관리자용: 특정 기록 상세 조회
    @Transactional(readOnly = true)
    public AdminRecordDetailResponse findRecordByIdForAdmin(Long id) {
        ReadingRecord record = readingRecordRepository.findByIdForAdmin(id)
                .orElseThrow(() -> new IllegalArgumentException("해당 기록을 찾을 수 없습니다. id=" + id));
        return new AdminRecordDetailResponse(record);
    }

    // 관리자용: 기록 수정 (userId 체크 없음)
    @Transactional
    public AdminRecordDetailResponse updateRecordForAdmin(Long id, AdminRecordUpdateRequest request) {
        ReadingRecord record = readingRecordRepository.findByIdForAdmin(id)
                .orElseThrow(() -> new IllegalArgumentException("해당 기록을 찾을 수 없습니다. id=" + id));

        // null이 아닌 필드만 업데이트
        if (request.getRawTitle() != null) record.setRawTitle(request.getRawTitle());
        if (request.getRawAuthor() != null) record.setRawAuthor(request.getRawAuthor());
        if (request.getSentence() != null) record.setSentence(request.getSentence());
        if (request.getComment() != null) record.setComment(request.getComment());
        record.setUpdatedAt(LocalDateTime.now());

        // 책 정보 변경 시 재매칭
        if (request.getRawTitle() != null || request.getRawAuthor() != null) {
            bookLinkService.removeBookMatch(id);

            if (present(record.getRawTitle()) && present(record.getRawAuthor())) {
                matchingBook(record);
            }
        }

        ReadingRecord saved = readingRecordRepository.save(record);
        return new AdminRecordDetailResponse(saved);
    }

    // 관리자용: 기록 삭제 (userId 체크 없음)
    @Transactional
    public void deleteRecordForAdmin(Long id) {
        ReadingRecord record = readingRecordRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("해당 기록을 찾을 수 없습니다. id=" + id));
        readingRecordRepository.delete(record);
    }

}
