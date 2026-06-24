package me.dodo.readingnotes.util;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * LLM 응답의 느슨한 JSON 파싱.
 * ```json 펜스 제거 → 직접 파싱 → 실패 시 {…} 구간 추출 재시도.
 */
public class LooseJson {

    private static final Logger log = LoggerFactory.getLogger(LooseJson.class);
    private static final ObjectMapper mapper = new ObjectMapper();

    public static JsonNode parse(String text) {
        if (text == null || text.isBlank()) return null;

        String s = text
                .replaceAll("(?s)```json\\s*", "")
                .replaceAll("(?s)```\\s*", "")
                .trim();

        try {
            return mapper.readTree(s);
        } catch (Exception ignored) {}

        int start = s.indexOf('{');
        int end = s.lastIndexOf('}');
        if (start >= 0 && end > start) {
            try {
                return mapper.readTree(s.substring(start, end + 1));
            } catch (Exception e) {
                log.warn("LooseJson.parse 실패. 앞부분: {}", s.substring(0, Math.min(200, s.length())));
            }
        }
        return null;
    }
}
