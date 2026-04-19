package me.dodo.readingnotes.dto.admin;

import java.time.LocalDate;
import java.util.List;

public class AdminRecordStatsResponse {

    private final long totalRecords;

    // recordedAt 기준 (사용자가 독서한 시각)
    private final long todayRecordCount;
    private final List<DailyCount> dailyRecordCounts;
    private final long activeUsersLast7Days;
    private final long activeUsersLast30Days;

    // createdAt 기준 (앱에 실제로 입력한 시각)
    private final long todayAppInputCount;
    private final List<DailyCount> dailyAppInputCounts;
    private final long activeAppInputUsersLast7Days;
    private final long activeAppInputUsersLast30Days;

    // 매칭 상태별
    private final long pendingCount;
    private final long resolvedAutoCount;
    private final long resolvedManualCount;
    private final long noCandidateCount;
    private final long multipleCandidatesCount;

    public AdminRecordStatsResponse(long totalRecords,
                                    long todayRecordCount,
                                    List<DailyCount> dailyRecordCounts,
                                    long activeUsersLast7Days,
                                    long activeUsersLast30Days,
                                    long todayAppInputCount,
                                    List<DailyCount> dailyAppInputCounts,
                                    long activeAppInputUsersLast7Days,
                                    long activeAppInputUsersLast30Days,
                                    long pendingCount,
                                    long resolvedAutoCount,
                                    long resolvedManualCount,
                                    long noCandidateCount,
                                    long multipleCandidatesCount) {
        this.totalRecords = totalRecords;
        this.todayRecordCount = todayRecordCount;
        this.dailyRecordCounts = dailyRecordCounts;
        this.activeUsersLast7Days = activeUsersLast7Days;
        this.activeUsersLast30Days = activeUsersLast30Days;
        this.todayAppInputCount = todayAppInputCount;
        this.dailyAppInputCounts = dailyAppInputCounts;
        this.activeAppInputUsersLast7Days = activeAppInputUsersLast7Days;
        this.activeAppInputUsersLast30Days = activeAppInputUsersLast30Days;
        this.pendingCount = pendingCount;
        this.resolvedAutoCount = resolvedAutoCount;
        this.resolvedManualCount = resolvedManualCount;
        this.noCandidateCount = noCandidateCount;
        this.multipleCandidatesCount = multipleCandidatesCount;
    }

    public long getTotalRecords() { return totalRecords; }
    public long getTodayRecordCount() { return todayRecordCount; }
    public List<DailyCount> getDailyRecordCounts() { return dailyRecordCounts; }
    public long getActiveUsersLast7Days() { return activeUsersLast7Days; }
    public long getActiveUsersLast30Days() { return activeUsersLast30Days; }
    public long getTodayAppInputCount() { return todayAppInputCount; }
    public List<DailyCount> getDailyAppInputCounts() { return dailyAppInputCounts; }
    public long getActiveAppInputUsersLast7Days() { return activeAppInputUsersLast7Days; }
    public long getActiveAppInputUsersLast30Days() { return activeAppInputUsersLast30Days; }
    public long getPendingCount() { return pendingCount; }
    public long getResolvedAutoCount() { return resolvedAutoCount; }
    public long getResolvedManualCount() { return resolvedManualCount; }
    public long getNoCandidateCount() { return noCandidateCount; }
    public long getMultipleCandidatesCount() { return multipleCandidatesCount; }

    public static class DailyCount {
        private final LocalDate date;
        private final long count;

        public DailyCount(LocalDate date, long count) {
            this.date = date;
            this.count = count;
        }

        public LocalDate getDate() { return date; }
        public long getCount() { return count; }
    }
}
