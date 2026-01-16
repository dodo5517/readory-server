package me.dodo.readingnotes.service;

import me.dodo.readingnotes.domain.UserAuthLog;
import me.dodo.readingnotes.dto.log.AuthLogDetailResponse;
import me.dodo.readingnotes.dto.log.AuthLogListResponse;
import me.dodo.readingnotes.repository.AuthLogRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class LogService {
    private final AuthLogRepository logRepository;

    public LogService(AuthLogRepository logRepository) {
        this.logRepository = logRepository;
    }

    // 전체 인증 로그 목록 조회
    @Transactional(readOnly = true)
    public Page<AuthLogListResponse> findAuthLogs(String keyword, UserAuthLog.AuthEventType type,
                                              UserAuthLog.AuthResult result, Pageable pageable) {
        System.out.println("srot: "+ pageable.getSort());
        String kw = normalize(keyword);
        String lowKw = (kw == null) ? null : "%" + kw.toLowerCase() + "%";

        return logRepository.searchLogs(lowKw, type, result, pageable)
                .map(AuthLogListResponse::new);
    }

    // 특정 인증 로그 조회
    @Transactional
    public AuthLogDetailResponse findAuthLog(Long id) {
        UserAuthLog log = logRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("해당 ID의 로그가 없습니다."));
        return new AuthLogDetailResponse(log);
    }

    private String normalize(String v) {
        if (v == null) return null;
        String t = v.trim();
        return t.isEmpty() ? null : t;
    }
}
