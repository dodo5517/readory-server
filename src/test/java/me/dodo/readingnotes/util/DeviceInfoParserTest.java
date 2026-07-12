package me.dodo.readingnotes.util;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class DeviceInfoParserTest {

    @Test
    @DisplayName("null User-Agent는 \"Unknown\"을 반환한다")
    void returnsUnknownForNull() {
        assertThat(DeviceInfoParser.extractDeviceInfo(null)).isEqualTo("Unknown");
    }

    @Test
    @DisplayName("빈 User-Agent는 \"Unknown\"을 반환한다")
    void returnsUnknownForEmpty() {
        assertThat(DeviceInfoParser.extractDeviceInfo("")).isEqualTo("Unknown");
    }

    @Test
    @DisplayName("정상 User-Agent는 \"OS / DeviceType / Browser\" 형식으로 반환한다")
    void returnsFormattedTriple() {
        String ua = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 "
                + "(KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36";

        String result = DeviceInfoParser.extractDeviceInfo(ua);

        // "OS / DeviceType / Browser" — 슬래시로 구분된 3개 토큰
        assertThat(result).contains(" / ");
        assertThat(result.split(" / ")).hasSize(3);
    }

    @Test
    @DisplayName("알 수 없는 형식의 User-Agent도 예외 없이 3토큰 형식을 반환한다")
    void handlesUnrecognizedUserAgentGracefully() {
        String result = DeviceInfoParser.extractDeviceInfo("some-garbage-agent-string");

        assertThat(result).isNotNull();
        assertThat(result.split(" / ")).hasSize(3);
    }
}