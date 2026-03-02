package me.dodo.readingnotes.dto.admin;

import java.time.LocalDate;
import java.util.List;

public class AdminRecordStatsResponse {

    private final long totalRecords;
    private final long todayRecords;
    private final long pendingCount;
    private final long resolvedAutoCount;
    private final long resolvedManualCount;
    private final long noCandidateCount;
    private final long multipleCandidatesCount;
    private final List<DailyCount> dailyCounts;
    private final long activeUsersLast7Days;
    private final long activeUsersLast30Days;

    public AdminRecordStatsResponse(long totalRecords, long todayRecords, long pendingCount,
                                    long resolvedAutoCount, long resolvedManualCount,
                                    long noCandidateCount, long multipleCandidatesCount,
                                    List<DailyCount> dailyCounts, long activeUsersLast7Days,
                                    long activeUsersLast30Days) {
        this.totalRecords = totalRecords;
        this.todayRecords = todayRecords;
        this.pendingCount = pendingCount;
        this.resolvedAutoCount = resolvedAutoCount;
        this.resolvedManualCount = resolvedManualCount;
        this.noCandidateCount = noCandidateCount;
        this.multipleCandidatesCount = multipleCandidatesCount;
        this.dailyCounts = dailyCounts;
        this.activeUsersLast7Days = activeUsersLast7Days;
        this.activeUsersLast30Days = activeUsersLast30Days;
    }

    public long getTotalRecords() {
        return totalRecords;
    }
    public long getTodayRecords() {
        return todayRecords;
    }
    public long getPendingCount() {
        return pendingCount;
    }
    public long getResolvedAutoCount() {
        return resolvedAutoCount;
    }
    public long getResolvedManualCount() {
        return resolvedManualCount;
    }
    public long getNoCandidateCount() {
        return noCandidateCount;
    }
    public long getMultipleCandidatesCount() {
        return multipleCandidatesCount;
    }
    public List<DailyCount> getDailyCounts() {
        return dailyCounts;
    }
    public long getActiveUsersLast7Days() {
        return activeUsersLast7Days;
    }
    public long getActiveUsersLast30Days() {
        return activeUsersLast30Days;
    }

    public static class DailyCount {
        private final LocalDate date;
        private final long count;

        public DailyCount(LocalDate date, long count) {
            this.date = date;
            this.count = count;
        }

        public LocalDate getDate() {
            return date;
        }

        public long getCount() {
            return count;
        }
    }
}