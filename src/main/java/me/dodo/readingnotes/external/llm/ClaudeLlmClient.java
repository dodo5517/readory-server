package me.dodo.readingnotes.external.llm;

import com.fasterxml.jackson.databind.JsonNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.Map;

/**
 * Anthropic Messages API 기반 LlmClient 구현.
 * - system 프롬프트에 cache_control(ephemeral)을 붙여 prompt caching 적용.
 *   (반복되는 고정 블록이 두 번째 호출부터 할인됨. 캐시는 Anthropic 측에 저장되어 서버 메모리와 무관.)
 * - external.llm.provider=api 일 때만 활성화. (기본값 api)
 */
@Component
@ConditionalOnProperty(name = "external.llm.provider", havingValue = "api", matchIfMissing = true)
public class ClaudeLlmClient implements LlmClient {

    private static final Logger log = LoggerFactory.getLogger(ClaudeLlmClient.class);

    private final RestClient restClient;
    private final String modelCheap;
    private final String modelQuality;

    public ClaudeLlmClient(
            @Qualifier("anthropicRestClient") RestClient restClient,
            @Value("${external.anthropic.model.cheap}") String modelCheap,
            @Value("${external.anthropic.model.quality}") String modelQuality) {
        this.restClient = restClient;
        this.modelCheap = modelCheap;
        this.modelQuality = modelQuality;
    }

    @Override
    public String complete(String system, String userText, int maxTokens, Tier tier) {
        String model = (tier == Tier.QUALITY) ? modelQuality : modelCheap;

        Map<String, Object> body = Map.of(
                "model", model,
                "max_tokens", maxTokens,
                // system을 배열로 주고 cache_control을 붙여 캐싱
                "system", List.of(Map.of(
                        "type", "text",
                        "text", system,
                        "cache_control", Map.of("type", "ephemeral")
                )),
                "messages", List.of(Map.of(
                        "role", "user",
                        "content", userText
                ))
        );

        JsonNode response = restClient.post()
                .uri("/v1/messages")
                .contentType(MediaType.APPLICATION_JSON)
                .body(body)
                .retrieve()
                .body(JsonNode.class);

        return extractText(response);
    }

    /** content 배열에서 type=text 블록들을 이어붙여 반환 */
    private String extractText(JsonNode response) {
        if (response == null || !response.has("content")) {
            log.warn("Claude 응답에 content가 없습니다: {}", response);
            return "";
        }
        StringBuilder sb = new StringBuilder();
        for (JsonNode block : response.get("content")) {
            if ("text".equals(block.path("type").asText())) {
                sb.append(block.path("text").asText());
            }
        }
        String text = sb.toString();
        // 잘림(max_tokens) 또는 빈 응답 진단용 로그
        String stopReason = response.path("stop_reason").asText("");
        if ("max_tokens".equals(stopReason)) {
            log.warn("Claude 응답이 max_tokens로 잘렸습니다. 출력 길이={}자", text.length());
        }
        if (text.isBlank()) {
            log.warn("Claude 텍스트 추출 결과가 비었습니다. stop_reason={}, 원문={}", stopReason, response);
        }
        return text;
    }
}