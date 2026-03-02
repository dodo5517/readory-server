package me.dodo.readingnotes.dto.admin;

import java.util.List;

public class AdminBookStatsResponse {

    private final List<TopBook> topByRecordCount;

    public AdminBookStatsResponse(List<TopBook> topByRecordCount) {
        this.topByRecordCount = topByRecordCount;
    }

    public List<TopBook> getTopByRecordCount() { return topByRecordCount; }
}