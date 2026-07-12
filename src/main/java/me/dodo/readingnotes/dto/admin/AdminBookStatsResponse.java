package me.dodo.readingnotes.dto.admin;

import java.util.List;

public record AdminBookStatsResponse(
        List<TopBook> topByRecordCount
) {}
