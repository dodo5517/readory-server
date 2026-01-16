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


    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void save(ApiLogCommand cmd) {
        try {
            ApiLog entity = new ApiLog();

            // user: 실패/비로그인/파싱 실패 등은 null
            entity.setUser(resolveUserOrNull(cmd.getUserId()));

            entity.setUserRole(safe(cmd.getUserRole(), 100));     // 엔티티 length=100
            entity.setMethod(safe(cmd.getMethod(), 100));
            entity.setPath(safe(cmd.getPath(), 255));
            entity.setQueryString(safe(cmd.getQueryString(), 255));

            entity.setStatusCode(cmd.getStatusCode());            // int라 length 무의미하지만 그대로 세팅
            entity.setResult(cmd.getResult() == null ? ApiLog.Result.FAIL : cmd.getResult());

            entity.setIpAddress(safe(cmd.getIpAddress(), 45));
            entity.setUserAgent(safe(cmd.getUserAgent(), 255));
            entity.setExecutionTimeMs(cmd.getExecutionTimeMs());

            entity.setErrorCode(safe(cmd.getErrorCode(), 20));
            entity.setErrorMessage(safe(cmd.getErrorMessage(), 100));

            apiLogRepository.save(entity);

        } catch (Exception e) {
            // 로그 저장 실패는 절대 본 요청을 망치면 안 됨
            throw new IllegalArgumentException("Failed to save api_logs: {}");
        }
    }

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
