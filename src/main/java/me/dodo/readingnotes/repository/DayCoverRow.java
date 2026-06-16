package me.dodo.readingnotes.repository;

import java.time.LocalDateTime;

public interface DayCoverRow {
    String getDay();
    String getCoverUrl();
    LocalDateTime getLastAt();
}
