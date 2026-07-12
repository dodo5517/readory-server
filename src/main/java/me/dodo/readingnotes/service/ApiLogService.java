package me.dodo.readingnotes.service;

import me.dodo.readingnotes.domain.ApiLog;
import me.dodo.readingnotes.domain.User;
import me.dodo.readingnotes.dto.log.ApiLogCommand;
import me.dodo.readingnotes.repository.ApiLogRepository;
import me.dodo.readingnotes.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ApiLogService {

    private final ApiLogRepository apiLogRepository;
    private final UserRepository userRepository;

    public ApiLogService(ApiLogRepository apiLogRepository,
                         UserRepository userRepository) {
        this.apiLogRepository = apiLogRepository;
        this.userRepository = userRepository;
    }

    // API 로그 저장
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void save(ApiLogCommand cmd) {
        try {
            // user: 실패/비로그인/파싱 실패 등은 null
            ApiLog entity = ApiLog.create(
                    resolveUserOrNull(cmd.getUserId()),
                    safe(cmd.getUserRole(), 100),     // 엔티티 length=100
                    safe(cmd.getMethod(), 100),
                    safe(cmd.getPath(), 255),
                    safe(cmd.getQueryString(), 255),
                    cmd.getStatusCode(),               // int라 length 무의미하지만 그대로 세팅
                    cmd.getResult() == null ? ApiLog.Result.FAIL : cmd.getResult(),
                    safe(cmd.getIpAddress(), 45),
                    safe(cmd.getUserAgent(), 255),
                    cmd.getExecutionTimeMs(),
                    safe(cmd.getErrorCode(), 20),
                    safe(cmd.getErrorMessage(), 100)
            );

            apiLogRepository.save(entity);

        } catch (Exception e) {
            // 로그 저장 실패는 절대 본 요청을 망치면 안 됨
            throw new IllegalArgumentException("Failed to save api_logs: {}");
        }
    }

    // 유저 정보 있는지 확인
    private User resolveUserOrNull(Long userId) {
        if (userId == null) return null;

        try {
            if (!userRepository.existsById(userId)) {
                return null;
            }
            return userRepository.getReferenceById(userId);
        } catch (Exception e) {
            return null;
        }
    }

    private String safe(String value, int maxLen) {
        if (value == null) return null;
        String v = value.trim();
        if (v.isEmpty()) return null;
        if (v.length() <= maxLen) return v;
        return v.substring(0, maxLen);
    }
}
