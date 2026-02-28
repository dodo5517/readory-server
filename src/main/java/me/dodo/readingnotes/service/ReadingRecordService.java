package me.dodo.readingnotes.service;

import jakarta.persistence.EntityNotFoundException;
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
import java.util.Optional;

@Service
public class ReadingRecordService {

    private final ReadingRecordRepository readingRecordRepository;
    private final UserRepository userRepository;
    private final BookRepository bookRepository;
    private final BookLinkService bookLinkService;
    private final BookMatchingAsyncService bookMatchingAsyncService;

    private static final Logger log = LoggerFactory.getLogger(ReadingRecordService.class);

    private static final int MAX_PAGE_SIZE = 30;
    private static final ZoneId ZONE = ZoneId.of("Asia/Seoul");

    @Autowired
    public ReadingRecordService(ReadingRecordRepository readingRecordRepository,
                                BookRepository bookRepository,
                                UserRepository userRepository,
                                BookLinkService bookLinkService,
                                BookMatchingAsyncService bookMatchingAsyncService) {
        this.readingRecordRepository = readingRecordRepository;
        this.userRepository = userRepository;
        this.bookRepository = bookRepository;
        this.bookLinkService = bookLinkService;
        this.bookMatchingAsyncService = bookMatchingAsyncService;
    }

    // 새로운 기록 생성 (User 객체를 Optional로 받아서 jwt, api 분리)
    @Transactional
    public ReadingRecord createByUserId(Long userId, User userFromFilter, ReadingRecordRequest req) {
        User user = userFromFilter != null
                ? userFromFilter
                : userRepository.findById(userId)
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
            // 책 검색 후 매칭 (비동기, 바로 리턴)
            bookMatchingAsyncService.matchAndSave(saved);
        }
        return saved;
    }
    private boolean present(String s) { return s != null && !s.isBlank(); }

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
    @Transactional
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
                // 책 검색 후 매칭 (비동기, 바로 리턴)
                bookMatchingAsyncService.matchAndSave(record);
            }
        }
        log.debug("saved record: {}", request.toString());
        // 수정한 기록 저장
        ReadingRecord saved = readingRecordRepository.save(record);

        // DTO로 변환
        return ReadingRecordResponse.fromEntity(saved);
    }

    // 기록 삭제
    @Transactional
    public void deleteRecordById(Long recordId, Long userId) {
        // 삭제하려는 행의 존재 여부 확인
        ReadingRecord record = readingRecordRepository.findByIdAndUserId(recordId, userId)
                .orElseThrow(()-> new IllegalArgumentException("해당 유저의 해당 레코드가 존재하지 않습니다: "+ userId +"의"+ recordId));
        // 삭제
        readingRecordRepository.delete(record);
    }

    // 해당 책의 모든 기록 삭제
    @Transactional
    public void deleteAllRecord(Long bookId, Long userId) {
        // 삭제할 기록의 존재 여부 확인
        boolean exists = readingRecordRepository.existsByBook_IdAndUser_Id(bookId, userId);
        if (!exists) {
            throw new IllegalArgumentException("해당 유저의 해당 책 기록이 존재하지 않습니다: " + userId + "의 " + bookId);
        }

        // 삭제
        readingRecordRepository.deleteAllByBookIdAndUserId(bookId, userId);
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
                // 책 검색 후 매칭 (비동기, 바로 리턴)
                bookMatchingAsyncService.matchAndSave(record);
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
