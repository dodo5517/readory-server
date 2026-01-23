package me.dodo.readingnotes.service;

import me.dodo.readingnotes.domain.ApiLog;
import me.dodo.readingnotes.domain.UserAuthLog;
import me.dodo.readingnotes.dto.log.ApiLogDetailResponse;
import me.dodo.readingnotes.dto.log.ApiLogListResponse;
import me.dodo.readingnotes.dto.log.AuthLogDetailResponse;
import me.dodo.readingnotes.dto.log.AuthLogListResponse;
import me.dodo.readingnotes.repository.ApiLogRepository;
import me.dodo.readingnotes.repository.AuthLogRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class LogService {
    private final AuthLogRepository authLogRepository;
    private final ApiLogRepository apiLogRepository;

    public LogService(AuthLogRepository authLogRepository,
                      ApiLogRepository apiLogRepository) {
        this.authLogRepository = authLogRepository;
        this.apiLogRepository = apiLogRepository;
    }

    // 전체 인증 로그 목록 조회
    @Transactional(readOnly = true)
    public Page<AuthLogListResponse> findAuthLogs(String keyword, UserAuthLog.AuthEventType type,
                                              UserAuthLog.AuthResult result, Pageable pageable) {
        System.out.println("srot: "+ pageable.getSort());
        String kw = normalize(keyword);
        String lowKw = (kw == null) ? null : "%" + kw.toLowerCase() + "%";

        return authLogRepository.searchLogs(lowKw, type, result, pageable)
                .map(AuthLogListResponse::new);
    }

    // 특정 인증 로그 조회
    @Transactional
    public AuthLogDetailResponse findAuthLog(Long id) {
        UserAuthLog log = authLogRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("해당 ID의 로그가 없습니다."));
        return new AuthLogDetailResponse(log);
    }

    // 전체 API 로그 조회
    @Transactional(readOnly = true)
    public Page<ApiLogListResponse> findApiLogs(
            String keyword,
            ApiLog.Result result,
            Integer statusCode,
            String method,
            Pageable pageable
    ) {
        String kw = normalize(keyword);
        String lowKw = (kw == null) ? null : "%" + kw.toLowerCase() + "%";

        String m = normalize(method);
        if (m != null) m = m.toUpperCase(); // GET/POST 보통 대문자라서 정규화

        return apiLogRepository.searchLogs(lowKw, result, statusCode, m, pageable)
                .map(ApiLogListResponse::new);
    }

    // 특정 API 로그 조회
    @Transactional(readOnly = true)
    public ApiLogDetailResponse findApiLog(Long id) {
        ApiLog log = apiLogRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("해당 ID의 로그가 없습니다."));
        return new ApiLogDetailResponse(log);
    }

    private String normalize(String v) {
        if (v == null) return null;
        String t = v.trim();
        return t.isEmpty() ? null : t;
    }

}
