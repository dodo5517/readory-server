package me.dodo.readingnotes.service;

import me.dodo.readingnotes.domain.ReadingRecord;
import me.dodo.readingnotes.dto.calendar.CalendarResponse;
import me.dodo.readingnotes.dto.calendar.CalendarSummary;
import me.dodo.readingnotes.dto.calendar.DayStat;
import me.dodo.readingnotes.repository.DayCountRow;
import me.dodo.readingnotes.repository.ReadingRecordRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;

@Service
public class ReadingCalendarService {

    private final ReadingRecordRepository repo;

    public ReadingCalendarService(ReadingRecordRepository repo) {
        this.repo = repo;
    }

    // 한 달 동안 기록한 날짜 조회(월간 달력용)
    public CalendarResponse getMonthly(Long userId, int year, int month) {
        YearMonth ym = YearMonth.of(year, month);
        LocalDate startDate = ym.atDay(1);
        LocalDate endDate = ym.atEndOfMonth();
        return buildResponse(userId, startDate, endDate);
    }

    // 연간 기록한 날짜 조회(연간 히트맵용)
    public CalendarResponse getYearly(Long userId, int year) {
        LocalDate startDate = LocalDate.of(year, 1, 1);
        LocalDate endDate = LocalDate.of(year, 12, 31);
        return buildResponse(userId, startDate, endDate);
    }

    // 공통 로직 분리
    private CalendarResponse buildResponse(Long userId, LocalDate startDate, LocalDate endDate) {
        // 그 달의 첫째 날
        LocalDateTime start = startDate.atStartOfDay();
        // 그 달의 마지막 날
        LocalDateTime end = endDate.plusDays(1).atStartOfDay();
        List<DayCountRow> rows = repo.countByDayInRange(userId, start, end);

        List<DayStat> days = new ArrayList<>();
        long totalRecords = 0;
        for (DayCountRow r : rows) {
            LocalDate d = LocalDate.parse(r.getDay()); // "YYYY-MM-DD"
            days.add(new DayStat(d, r.getCnt()));
            totalRecords += r.getCnt();
        }

        int totalDaysWithRecord = days.size();
        String first = totalDaysWithRecord == 0 ? null : days.get(0).getDate().toString();
        String last  = totalDaysWithRecord == 0 ? null : days.get(totalDaysWithRecord - 1).getDate().toString();

        CalendarSummary summary = new CalendarSummary(totalDaysWithRecord, totalRecords, first, last);
        return new CalendarResponse(startDate, endDate, days, summary);
    }

    // 하루 기록 보기
    public Page<ReadingRecord> findByDay(Long userId, LocalDate day, String q, Pageable pageable) {
        LocalDateTime start = day.atStartOfDay();
        LocalDateTime end   = day.plusDays(1).atStartOfDay();
        return repo.findRecordsInRange(userId, start, end, q, pageable);
    }
    // 월 전체 기록 보기
    public Page<ReadingRecord> findByMonth(Long userId, int year, int month, String q, Pageable pageable) {
        YearMonth ym = YearMonth.of(year, month);
        LocalDateTime start = ym.atDay(1).atStartOfDay();
        LocalDateTime end   = ym.plusMonths(1).atDay(1).atStartOfDay();
        return repo.findRecordsInRange(userId, start, end, q, pageable);
    }
}
