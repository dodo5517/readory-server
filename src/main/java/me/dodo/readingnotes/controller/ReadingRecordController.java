package me.dodo.readingnotes.controller;

import jakarta.servlet.http.HttpServletRequest;
import me.dodo.readingnotes.config.ApiKeyFilter;
import me.dodo.readingnotes.domain.ReadingRecord;
import me.dodo.readingnotes.domain.User;
import me.dodo.readingnotes.dto.book.BookRecordsPageResponse;
import me.dodo.readingnotes.dto.book.BookWithLastRecordResponse;
import me.dodo.readingnotes.dto.calendar.CalendarResponse;
import me.dodo.readingnotes.dto.common.ApiResponse;
import me.dodo.readingnotes.dto.common.PageResponse;
import me.dodo.readingnotes.dto.reading.ReadingRecordRequest;
import me.dodo.readingnotes.dto.reading.ReadingRecordResponse;
import me.dodo.readingnotes.exception.AuthException;
import me.dodo.readingnotes.service.ReadingCalendarService;
import me.dodo.readingnotes.service.ReadingRecordService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.*;
import org.springframework.data.web.PageableDefault;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/records")
public class ReadingRecordController {
    private static final Logger log = LoggerFactory.getLogger(ReadingRecordController.class);
    private static final int MAX_SIZE = 30;

    private int clampSize(int size) {
        return Math.min(Math.max(size, 1), MAX_SIZE);
    }

    private final ReadingRecordService service;
    private final ReadingCalendarService calendarService;

    public ReadingRecordController(ReadingRecordService service,
                                   ReadingCalendarService calendarService) {
        this.service = service;
        this.calendarService = calendarService;
    }

    // 아이폰 단축어로 메모 추가
    @PostMapping
    public ApiResponse<String> create(HttpServletRequest request,
                                      @RequestBody ReadingRecordRequest req) {
        User user = (User) request.getAttribute("apiUser");
        Long userId = (Long) request.getAttribute(ApiKeyFilter.ATTR_API_USER_ID);
        ReadingRecord saved = service.createByUserId(userId, user, req);
        return ApiResponse.success(null, "문장: " + saved.getSentence() + "\n메모: " + saved.getComment() + "\n기록을 저장했습니다.");
    }

    // 웹에서 메모 추가
    @PostMapping("/web")
    public ApiResponse<Void> webCreate(HttpServletRequest request,
                                       @RequestBody ReadingRecordRequest req) {
        Long userId = resolveUserId(request);
        service.createByUserId(userId, null, req);
        return ApiResponse.success("기록이 저장되었습니다.");
    }

    // 해당 유저의 최근 N(default=3)개 기록 조회(메인 화면용)
    @GetMapping("/me/summary")
    public ApiResponse<List<ReadingRecordResponse>> getMyLatestRecords(
            HttpServletRequest request,
            @RequestParam(value = "size", defaultValue = "3") int size) {
        Long userId = resolveUserId(request);
        size = clampSize(size);
        List<ReadingRecord> list = service.getLatestRecords(userId, size);
        log.debug("list: {}", list.toString());
        return ApiResponse.success(list.stream().map(ReadingRecordResponse::new).collect(Collectors.toList()));
    }

    // 해당 유저의 모든 기록 조회
    @GetMapping("/me")
    public ApiResponse<PageResponse<ReadingRecordResponse>> getMyRecords(
            HttpServletRequest request,
            @PageableDefault(size = 10, direction = Sort.Direction.DESC) Pageable pageable,
            @RequestParam(value = "scope", defaultValue = "titleAndAuthor") String scope,
            @RequestParam(value = "q", required = false) String q) {
        Long userId = resolveUserId(request);
        Page<ReadingRecord> page = service.getMyRecords(userId, scope, q, pageable);
        return ApiResponse.success(PageResponse.from(page.map(ReadingRecordResponse::new)));
    }

    // 해당 유저가 읽은 책 중 매핑이 끝난 N(default=20)개 책들 조회
    @GetMapping("/me/books")
    public ApiResponse<PageResponse<BookWithLastRecordResponse>> getMyConfirmedBooks(
            HttpServletRequest request,
            @RequestParam(value = "q", required = false) String q,
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "10") int size,
            @RequestParam(value = "sort", defaultValue = "recent") String sort) {
        Long userId = resolveUserId(request);
        size = clampSize(size);
        Pageable pageable = PageRequest.of(page, size);
        return ApiResponse.success(PageResponse.from(service.getConfirmedBooks(userId, q, pageable, sort)));
    }

    // 메인 화면용 - 핀 무시하고 순수 최신순
    @GetMapping("/me/books/main")
    public ApiResponse<PageResponse<BookWithLastRecordResponse>> getMyBooksForMain(
            HttpServletRequest request,
            @RequestParam(value = "q", required = false) String q,
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "10") int size) {
        Long userId = resolveUserId(request);
        size = clampSize(size);
        Pageable pageable = PageRequest.of(page, size);
        return ApiResponse.success(PageResponse.from(service.getConfirmedBooksForMain(userId, q, pageable)));
    }

    // 해당 유저가 기록한 책 한 권에 대한 모든 기록 조회
    @GetMapping("/books/{bookId}")
    public ApiResponse<BookRecordsPageResponse> getBookRecords(
            @PathVariable("bookId") Long bookId,
            @RequestParam(required = false) String cursor,
            @RequestParam(defaultValue = "20") int size,
            HttpServletRequest request) {
        Long userId = resolveUserId(request);
        return ApiResponse.success(service.getBookRecordsByCursor(userId, bookId, cursor, size));
    }

    // 한 달 / 연간 기록한 날짜 조회
    @GetMapping("/calendar")
    public ApiResponse<CalendarResponse> getCalendar(
            @RequestParam(value = "year") int year,
            @RequestParam(value = "month") int month,
            HttpServletRequest request) {
        Long userId = resolveUserId(request);
        if (month == 0) {
            return ApiResponse.success(calendarService.getYearly(userId, year));
        }
        return ApiResponse.success(calendarService.getMonthly(userId, year, month));
    }

    // 월 기록 목록 조회
    @GetMapping("/month")
    public ApiResponse<PageResponse<ReadingRecordResponse>> getMyMonth(
            @RequestParam(value = "year") int year,
            @RequestParam(value = "month") int month,
            @RequestParam(value = "q", required = false) String q,
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "10") int size,
            @RequestParam(value = "sort", defaultValue = "desc") String sort,
            HttpServletRequest request) {
        Long userId = resolveUserId(request);
        size = clampSize(size);
        Sort order = "asc".equalsIgnoreCase(sort)
                ? Sort.by("recordedAt").ascending()
                : Sort.by("recordedAt").descending();
        Pageable pageable = PageRequest.of(page, size, order);
        return ApiResponse.success(PageResponse.from(calendarService.findByMonth(userId, year, month, q, pageable)));
    }

    // 하루 기록 목록 조회
    @GetMapping("/day")
    public ApiResponse<PageResponse<ReadingRecordResponse>> getMyDay(
            @RequestParam(value = "date") String date,
            @RequestParam(value = "q", required = false) String q,
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "10") int size,
            @RequestParam(value = "sort", defaultValue = "desc") String sort,
            HttpServletRequest request) {
        Long userId = resolveUserId(request);
        size = clampSize(size);
        Sort order = "asc".equalsIgnoreCase(sort)
                ? Sort.by("recordedAt").ascending()
                : Sort.by("recordedAt").descending();
        Pageable pageable = PageRequest.of(page, size, order);
        return ApiResponse.success(PageResponse.from(calendarService.findByDay(userId, LocalDate.parse(date), q, pageable)));
    }

    // 기록 수정
    @PostMapping("/update/{recordId}")
    public ApiResponse<ReadingRecordResponse> updateRecord(
            @PathVariable("recordId") Long recordId,
            @RequestBody ReadingRecordRequest req,
            HttpServletRequest request) {
        Long userId = resolveUserId(request);
        return ApiResponse.success(service.update(recordId, userId, req));
    }

    // 기록 삭제
    @DeleteMapping("/delete/{recordId}")
    public ApiResponse<Void> deleteRecord(
            @PathVariable Long recordId,
            HttpServletRequest request) {
        Long userId = resolveUserId(request);
        service.deleteRecordById(recordId, userId);
        return ApiResponse.success("기록이 삭제되었습니다.");
    }

    // 해당 책의 모든 기록 삭제
    @DeleteMapping("/delete/books/{bookId}")
    public ApiResponse<Void> deleteAllRecord(
            @PathVariable Long bookId,
            HttpServletRequest request) {
        Long userId = resolveUserId(request);
        service.deleteAllRecord(bookId, userId);
        return ApiResponse.success("책의 모든 기록이 삭제되었습니다.");
    }

    private Long resolveUserId(HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("USER_ID");
        if (userId == null) {
            throw new AuthException("인증이 필요합니다.");
        }
        return userId;
    }
}
