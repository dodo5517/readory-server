package me.dodo.readingnotes.config;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import me.dodo.readingnotes.domain.ApiLog;
import me.dodo.readingnotes.dto.log.ApiLogCommand;
import me.dodo.readingnotes.service.ApiLogService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.concurrent.ThreadLocalRandom;

@Component
public class ApiLogInterceptor implements HandlerInterceptor {
    private static final String ATTR_START_TIME = "API_LOG_START_TIME";

    // 운영 정책
    @Value("${api.log.slow-threshold-ms}")
    private int slowThresholdMs;

    @Value("${api.log.success-enabled}")
    private boolean successLogEnabled;

    private final ApiLogService apiLogService;
    private final ApiLogRequestInfoExtractor extractor;

    public ApiLogInterceptor(ApiLogService apiLogService,
                             ApiLogRequestInfoExtractor extractor) {
        this.apiLogService = apiLogService;
        this.extractor = extractor;
    }

    // 시작 시간
    @Override
    public boolean preHandle(HttpServletRequest request,
                             HttpServletResponse response,
                             Object handler) {

        request.setAttribute(ATTR_START_TIME, System.currentTimeMillis());
        return true;
    }

    // ApiLogCommand 생성(service에 넘겨줄 형태)
    @Override
    public void afterCompletion(HttpServletRequest request,
                                HttpServletResponse response,
                                Object handler,
                                Exception ex) {
        // 저장 제외(노이즈 제거)
        if (shouldSkip(request)) {
            return;
        }

        Object startAttr = request.getAttribute(ATTR_START_TIME);
        if (!(startAttr instanceof Long)) {
            startAttr = System.currentTimeMillis();
        }

        long start = (long) request.getAttribute(ATTR_START_TIME);
        int executionTimeMs = (int) (System.currentTimeMillis() - start);

        int statusCode = response.getStatus();
        ApiLog.Result result =
                (ex == null && statusCode < 400)
                        ? ApiLog.Result.SUCCESS
                        : ApiLog.Result.FAIL;

        // 저장량 제어 핵심 정책
        if (!shouldStore(result, executionTimeMs)) {
            return;
        }

        ApiLogCommand command = new ApiLogCommand(
                extractor.extractUserIdOrNull(request),
                extractor.extractUserRoleOrNull(request),
                request.getMethod(),
                extractor.extractPath(request),
                extractor.extractQueryString(request),
                statusCode,
                result,
                extractor.extractClientIp(request),
                extractor.extractUserAgent(request),
                executionTimeMs,
                extractor.extractErrorCode(ex, statusCode),
                extractor.extractErrorMessage(ex, statusCode)
        );

        apiLogService.save(command);
    }
    private boolean shouldSkip(HttpServletRequest request) {
        String method = request.getMethod();
        String path = request.getRequestURI();

        // CORS preflight
        if ("OPTIONS".equalsIgnoreCase(method)) return true;

        // 운영/문서/노이즈 경로
        if (path == null) return false;
        return path.equals("/error")
                || path.startsWith("/actuator")
                || path.startsWith("/swagger-ui")
                || path.startsWith("/v3/api-docs")
                || path.startsWith("/favicon.ico")
                || path.startsWith("/css")
                || path.startsWith("/js")
                || path.startsWith("/images");
    }

    // FAIL 100% 저장, 느린 요청은 성공이어도 100% 저장, 그 외 성공 요청은 샘플링 저장
    private boolean shouldStore(ApiLog.Result result, int executionTimeMs) {
        // 실패(예외/4xx/5xx)는 무조건 저장
        if (result == ApiLog.Result.FAIL) return true;

        // 느린 요청은 성공이어도 무조건 저장
        if (executionTimeMs >= slowThresholdMs) return true;

        // 그 외 성공 요청은 샘플링
        return successLogEnabled;
    }
}