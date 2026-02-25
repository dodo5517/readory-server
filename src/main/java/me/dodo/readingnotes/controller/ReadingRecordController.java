package me.dodo.readingnotes.controller;

import jakarta.servlet.http.HttpServletRequest;
import me.dodo.readingnotes.config.ApiKeyFilter;
import me.dodo.readingnotes.domain.ReadingRecord;
import me.dodo.readingnotes.dto.book.BookRecordsPageResponse;
import me.dodo.readingnotes.dto.book.BookWithLastRecordResponse;
import me.dodo.readingnotes.dto.calendar.CalendarResponse;
import me.dodo.readingnotes.dto.reading.ReadingRecordRequest;
import me.dodo.readingnotes.dto.reading.ReadingRecordResponse;
import me.dodo.readingnotes.service.ReadingCalendarService;
import me.dodo.readingnotes.service.ReadingRecordService;
import me.dodo.readingnotes.util.JwtTokenProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.*;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/records")
public class ReadingRecordController {
    private static final Logger log = LoggerFactory.getLogger(ReadingRecordController.class);

    private final ReadingRecordService service;
    private final JwtTokenProvider jwtTokenProvider;
    private final ReadingCalendarService calendarService;

    public ReadingRecordController(ReadingRecordService service, JwtTokenProvider jwtTokenProvider,
                                   ReadingCalendarService calendarService) {
        this.service = service;
        this.jwtTokenProvider = jwtTokenProvider;
        this.calendarService = calendarService;
    }

    // 아이폰 단축어로 메모 추가
    @PostMapping
    public ResponseEntity<String> create(HttpServletRequest request,
                                       @RequestBody ReadingRecordRequest req) {
        Long userId = (Long) request.getAttribute(ApiKeyFilter.ATTR_API_USER_ID);
        if (userId == null) {
            return ResponseEntity.status(401).build(); // 필터가 보통 막지만 방어
        }
//        log.debug("Create Record, userID={}", userId);
        ReadingRecord saved = service.createByUserId(userId, req);
        return ResponseEntity.ok("문장: "+saved.getSentence()+"\n메모: "+saved.getComment()+"\n"+"기록을 저장했습니다.");
    }

    // 웹에서 메모 추가
    @PostMapping("/web")
    public ResponseEntity<Void> webCreate(HttpServletRequest request,
                                       @RequestBody ReadingRecordRequest req) {
        Long userId = (Long) request.getAttribute("USER_ID");
        if (userId == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "인증이 필요합니다.");
        }
//        log.debug("Create Record, userID={}", userId);
        service.createByUserId(userId, req);
        // 상태코드만 반환 (204)
        return ResponseEntity.noContent().build();
    }

    // 해당 유저의 최근 N(default=3)개 기록 조회(메인 화면용)
    @GetMapping("/me/summary")
    public  List<ReadingRecordResponse> getMyLatestRecords(
            HttpServletRequest request,
            @RequestParam(value = "size", defaultValue = "3") int size
    ){
        // JwtAuthFilter가 심어준 값 사용
        Long userId = (Long) request.getAttribute("USER_ID");
        if (userId == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "인증이 필요합니다.");
        }

        List<ReadingRecord> list = service.getLatestRecords(userId, size);
        log.debug("list: {}", list.toString());
        return list.stream().map(ReadingRecordResponse::new).collect(Collectors.toList());
    }

    // 해당 유저의 모든 기록 조회
    @GetMapping("/me")
    // Page로 반환하므로 관련된 메타데이터도 따로 전달됨.
    public Page<ReadingRecordResponse> getMyRecords(
            HttpServletRequest request,
            @PageableDefault(size = 10, direction = Sort.Direction.DESC) Pageable pageable,
            @RequestParam(value = "scope", defaultValue = "titleAndAuthor") String scope,
            @RequestParam(value = "q", required = false) String q
    ) {
        // JwtAuthFilter가 심어준 값 사용
        Long userId = (Long) request.getAttribute("USER_ID");
        if (userId == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "인증이 필요합니다.");
        }

        Page<ReadingRecord> page = service.getMyRecords(userId, scope, q, pageable);
        return page.map(ReadingRecordResponse::new);
    }

    // 해당 유저가 읽은 책 중 매핑이 끝난 N(default=20)개 책들 조회
    @GetMapping("/me/books")
    public Page<BookWithLastRecordResponse> getMyConfirmedBooks(
            HttpServletRequest request,
            @RequestParam(value = "q",required = false) String q,
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "10") int size,
            @RequestParam(value = "sort", defaultValue = "recent") String sort
    ) {
        // JwtAuthFilter가 심어준 값 사용
        Long userId = (Long) request.getAttribute("USER_ID");
        if (userId == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "인증이 필요합니다.");
        }

        Pageable pageable = PageRequest.of(page, size);

        return service.getConfirmedBooks(userId, q, pageable, sort);
    }

    // 해당 유저가 기록한 책 한 권에 대한 모든 기록 조회
    @GetMapping("/books/{bookId}")
    public BookRecordsPageResponse getBookRecords(
            @PathVariable("bookId") Long bookId,
            @RequestParam(required = false) String cursor,
            @RequestParam(defaultValue = "20") int size,
            HttpServletRequest request
    ) {
        // JwtAuthFilter가 심어준 값 사용
        Long userId = (Long) request.getAttribute("USER_ID");
        if (userId == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "인증이 필요합니다.");
        }

        return service.getBookRecordsByCursor(userId, bookId, cursor, size);
    }

    // 한 달 동안 기록한 날짜 조회
    @GetMapping("/calendar")
    public CalendarResponse getCalendar(@RequestParam(value = "year") int year,
                                        @RequestParam(value = "month") int month,
                                        HttpServletRequest request) {
        // JwtAuthFilter가 심어준 값 사용
        Long userId = (Long) request.getAttribute("USER_ID");
        if (userId == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "인증이 필요합니다.");
        }

        if (month == 0) {
            return calendarService.getYearly(userId, year);   // 연간 히트맵용
        }
        return calendarService.getMonthly(userId, year, month); // 월간 달력용
    }

    // 월 기록 목록 조회
    @GetMapping("/month")
    public Page<ReadingRecordResponse> getMyMonth(@RequestParam(value = "year") int year,
                                                  @RequestParam(value = "month") int month,
                                                  @RequestParam(value = "q", required = false) String q,
                                                  @RequestParam(value = "page", defaultValue = "0") int page,
                                                  @RequestParam(value = "size", defaultValue = "10") int size, // 기본 10개
                                                  @RequestParam(value = "sort", defaultValue = "desc") String sort, // 기본 내림차순
                                                  HttpServletRequest request) {
        // JwtAuthFilter가 심어준 값 사용
        Long userId = (Long) request.getAttribute("USER_ID");
        if (userId == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "인증이 필요합니다.");
        }

        // 날짜 오름/내림으로만 정렬 가능함.
        Sort order = "asc".equalsIgnoreCase(sort)
                ? Sort.by("recordedAt").ascending()
                : Sort.by("recordedAt").descending();

        Pageable pageable = PageRequest.of(page, size, order);
        Page<ReadingRecord> pageAndRecords = calendarService.findByMonth(userId, year, month, q, pageable);
        return pageAndRecords.map(ReadingRecordResponse::new);
    }
    // 하루 기록 목록 조회
    @GetMapping("/day")
    public Page<ReadingRecordResponse> getMyDay(@RequestParam(value = "date") String date,
                                                  @RequestParam(value = "q", required = false) String q,
                                                  @RequestParam(value = "page", defaultValue = "0") int page,
                                                  @RequestParam(value = "size", defaultValue = "10") int size, // 기본 10개
                                                  @RequestParam(value = "sort", defaultValue = "desc") String sort, // 기본 내림차순
                                                  HttpServletRequest request) {
        // JwtAuthFilter가 심어준 값 사용
        Long userId = (Long) request.getAttribute("USER_ID");
        if (userId == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "인증이 필요합니다.");
        }


        // 날짜 오름/내림으로만 정렬 가능함.
        Sort order = "asc".equalsIgnoreCase(sort)
                ? Sort.by("recordedAt").ascending()
                : Sort.by("recordedAt").descending();

        Pageable pageable = PageRequest.of(page, size, order);
        Page<ReadingRecord> pageAndRecords = calendarService.findByDay(userId, LocalDate.parse(date), q, pageable);
        return pageAndRecords.map(ReadingRecordResponse::new);
    }

    // 기록 수정
    @PostMapping("/update/{recordId}")
    public ReadingRecordResponse updateRecord(
            @PathVariable("recordId") Long recordId,
            @RequestBody ReadingRecordRequest req,
            HttpServletRequest request){
        // JwtAuthFilter가 심어준 값 사용
        Long userId = (Long) request.getAttribute("USER_ID");
        if (userId == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "인증이 필요합니다.");
        }

        return service.update(recordId, userId, req);
    }

    // 기록 삭제
    @DeleteMapping("/delete/{recordId}")
    public void deleteRecord(
            @PathVariable Long recordId,
            HttpServletRequest request){
        // JwtAuthFilter가 심어준 값 사용
        Long userId = (Long) request.getAttribute("USER_ID");
        if (userId == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "인증이 필요합니다.");
        }

        service.deleteRecordById(recordId, userId);
    }
}
