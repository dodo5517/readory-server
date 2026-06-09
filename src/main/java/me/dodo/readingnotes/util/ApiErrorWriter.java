package me.dodo.readingnotes.util;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletResponse;
import me.dodo.readingnotes.dto.common.ApiResponse;

import java.io.IOException;

public class ApiErrorWriter {

    public static void writeApiError(HttpServletResponse response,
                                     ObjectMapper objectMapper,
                                     int status,
                                     String code,
                                     String message) throws IOException {
        response.setStatus(status);
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        objectMapper.writeValue(response.getWriter(), ApiResponse.error(code, message));
    }
}
