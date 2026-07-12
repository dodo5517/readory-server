package me.dodo.readingnotes.dto.admin;

import java.time.LocalDate;
import java.util.List;

public record AdminRecordStatsResponse(
        long totalRecords,

        // recordedAt 기준 (사용자가 독서한 시각)
        long todayRecordCount,
        List<DailyCount> dailyRecordCounts,
        long activeUsersLast7Days,
        long activeUsersLast30Days,

        // createdAt 기준 (앱에 실제로 입력한 시각)
        long todayAppInputCount,
        List<DailyCount> dailyAppInputCounts,
        long activeAppInputUsersLast7Days,
        long activeAppInputUsersLast30Days,

        // 매칭 상태별
        long pendingCount,
        long resolvedAutoCount,
        long resolvedManualCount,
        long noCandidateCount,
        long multipleCandidatesCount
) {
    public record DailyCount(LocalDate date, long count) {}
}
