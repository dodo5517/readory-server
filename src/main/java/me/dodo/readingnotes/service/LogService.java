package me.dodo.readingnotes.service;

import me.dodo.readingnotes.domain.UserAuthLog;
import me.dodo.readingnotes.dto.log.LogDetailResponse;
import me.dodo.readingnotes.dto.log.LogListResponse;
import me.dodo.readingnotes.repository.LogRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class LogService {
    private final LogRepository logRepository;

    public LogService(LogRepository logRepository) {
        this.logRepository = logRepository;
    }

    // 전체 로그 목록 조회
    @Transactional(readOnly = true)
    public Page<LogListResponse> findLogs(String keyword, UserAuthLog.AuthEventType type,
                                          UserAuthLog.AuthResult result, Pageable pageable) {
        System.out.println("srot: "+ pageable.getSort());
        String kw = normalize(keyword);
        String lowKw = (kw == null) ? null : "%" + kw.toLowerCase() + "%";

        return logRepository.searchLogs(lowKw, type, result, pageable)
                .map(LogListResponse::new);
    }

    // 특정 로그 조회
    @Transactional
    public LogDetailResponse findLog(Long id) {
        UserAuthLog log = logRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("해당 ID의 로그가 없습니다."));
        return new LogDetailResponse(log);
    }

    private String normalize(String v) {
        if (v == null) return null;
        String t = v.trim();
        return t.isEmpty() ? null : t;
    }
}
