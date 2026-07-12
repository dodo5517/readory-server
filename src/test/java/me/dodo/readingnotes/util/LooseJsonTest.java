package me.dodo.readingnotes.util;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class LooseJsonTest {

    @Test
    @DisplayName("순수 JSON 문자열을 그대로 파싱한다")
    void parsesPlainJson() {
        JsonNode node = LooseJson.parse("{\"key\":\"value\"}");

        assertThat(node).isNotNull();
        assertThat(node.get("key").asText()).isEqualTo("value");
    }

    @Test
    @DisplayName("```json 코드펜스로 감싼 응답에서 JSON을 추출한다")
    void stripsJsonCodeFence() {
        String raw = "```json\n{\"a\":1}\n```";

        JsonNode node = LooseJson.parse(raw);

        assertThat(node).isNotNull();
        assertThat(node.get("a").asInt()).isEqualTo(1);
    }

    @Test
    @DisplayName("``` 코드펜스(언어 표기 없음)도 제거한다")
    void stripsBareCodeFence() {
        JsonNode node = LooseJson.parse("```\n{\"a\":1}\n```");

        assertThat(node).isNotNull();
        assertThat(node.get("a").asInt()).isEqualTo(1);
    }

    @Test
    @DisplayName("앞뒤에 설명 텍스트가 섞여 있어도 {…} 구간을 추출해 파싱한다")
    void extractsJsonFromSurroundingText() {
        String raw = "다음은 결과입니다 {\"k\":\"v\"} 이상입니다.";

        JsonNode node = LooseJson.parse(raw);

        assertThat(node).isNotNull();
        assertThat(node.get("k").asText()).isEqualTo("v");
    }

    @Test
    @DisplayName("중첩 객체가 섞여 있어도 가장 바깥 {…} 구간을 추출한다")
    void extractsOutermostObject() {
        String raw = "텍스트 {\"outer\":{\"inner\":2}} 끝";

        JsonNode node = LooseJson.parse(raw);

        assertThat(node).isNotNull();
        assertThat(node.get("outer").get("inner").asInt()).isEqualTo(2);
    }

    @Test
    @DisplayName("null 입력은 null을 반환한다")
    void returnsNullForNullInput() {
        assertThat(LooseJson.parse(null)).isNull();
    }

    @Test
    @DisplayName("빈 문자열/공백 입력은 null을 반환한다")
    void returnsNullForBlankInput() {
        assertThat(LooseJson.parse("")).isNull();
        assertThat(LooseJson.parse("   ")).isNull();
    }

    @Test
    @DisplayName("JSON 객체 구간이 전혀 없는 문자열은 null을 반환한다")
    void returnsNullWhenNoJsonObject() {
        assertThat(LooseJson.parse("이건 그냥 평범한 텍스트입니다")).isNull();
    }
}