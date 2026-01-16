package me.dodo.readingnotes.config;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import me.dodo.readingnotes.domain.ApiLog;
import me.dodo.readingnotes.dto.log.ApiLogCommand;
import me.dodo.readingnotes.service.ApiLogService;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
public class ApiLogInterceptor implements HandlerInterceptor {
    private static final String ATTR_START_TIME = "API_LOG_START_TIME";

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
//        System.out.println("[API_LOG] afterCompletion: " + request.getMethod() + " " + request.getRequestURI());
        long start = (long) request.getAttribute(ATTR_START_TIME);
        int executionTimeMs = (int) (System.currentTimeMillis() - start);

        int statusCode = response.getStatus();
        ApiLog.Result result =
                (ex == null && statusCode < 400)
                        ? ApiLog.Result.SUCCESS
                        : ApiLog.Result.FAIL;

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
}