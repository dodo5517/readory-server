package me.dodo.readingnotes.external.llm;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * 로컬에 설치된 Claude Code CLI(`claude -p`)를 호출하는 LlmClient 구현.
 * - 테스트/개발용. API 토큰 대신 로그인된 Claude 구독 세션을 사용한다.
 * - external.llm.provider=cli 일 때만 활성화.
 *
 * 주의:
 * - 백엔드가 도는 머신에 claude CLI가 설치되어 있고 로그인되어 있어야 한다.
 * - 매 호출마다 프로세스를 새로 띄우므로 API보다 느리다(부팅 오버헤드).
 * - --system-prompt로 기본 프롬프트를 우리 system으로 교체하고, user 텍스트는 stdin으로 넘긴다.
 */
@Component
@ConditionalOnProperty(name = "external.llm.provider", havingValue = "cli")
public class ClaudeCliClient implements LlmClient {

    private static final Logger log = LoggerFactory.getLogger(ClaudeCliClient.class);

    private final String cliPath;
    private final String modelCheap;
    private final String modelQuality;
    private final long timeoutMs;
    private static final ObjectMapper MAPPER = new ObjectMapper();

    public ClaudeCliClient(
            @Value("${external.llm.cli.path:claude}") String cliPath,
            @Value("${external.llm.cli.model.cheap:haiku}") String modelCheap,
            @Value("${external.llm.cli.model.quality:sonnet}") String modelQuality,
            @Value("${external.llm.cli.timeout-ms:180000}") long timeoutMs) {
        this.cliPath = cliPath;
        this.modelCheap = modelCheap;
        this.modelQuality = modelQuality;
        this.timeoutMs = timeoutMs;
    }

    @Override
    public String complete(String system, String userText, int maxTokens, Tier tier) {
        String model = (tier == Tier.QUALITY) ? modelQuality : modelCheap;

        List<String> command = new ArrayList<>();
        command.add(cliPath);
        command.add("-p");                       // headless(print) 모드
        command.add("--model");
        command.add(model);                      // 별칭(haiku/sonnet) — 스크립트엔 별칭이 안전
        command.add("--system-prompt");
        command.add(system);                     // 기본 프롬프트를 우리 system으로 교체
        command.add("--output-format");
        command.add("json");                     // 구조화 출력 → result 필드에서 순수 응답 추출
        command.add("--max-turns");
        command.add("1");                        // 한 번에 끝(에이전트 루프 방지)
        // user 메시지는 stdin으로 전달(따옴표/개행 이스케이프 문제 회피)

        Process process = null;
        try {
            ProcessBuilder pb = new ProcessBuilder(command);
            pb.redirectErrorStream(false);
            process = pb.start();

            // user 텍스트를 stdin으로 주입
            try (OutputStream stdin = process.getOutputStream()) {
                stdin.write(userText.getBytes(StandardCharsets.UTF_8));
                stdin.flush();
            }

            // stdout 읽기
            StringBuilder out = new StringBuilder();
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    out.append(line).append("\n");
                }
            }

            boolean finished = process.waitFor(timeoutMs, TimeUnit.MILLISECONDS);
            if (!finished) {
                process.destroyForcibly();
                throw new RuntimeException("claude CLI 응답 시간 초과(" + timeoutMs + "ms)");
            }

            int exit = process.exitValue();
            if (exit != 0) {
                String err = readStream(process);
                log.error("claude CLI 비정상 종료 code={} stderr={}", exit, err);
                throw new RuntimeException("claude CLI 호출 실패(exit=" + exit + ")");
            }

            String stdout = out.toString().strip();
            return extractResult(stdout);

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("claude CLI 호출 중단", e);
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException("claude CLI 호출 오류: " + e.getMessage(), e);
        } finally {
            if (process != null && process.isAlive()) {
                process.destroyForcibly();
            }
        }
    }

    /** --output-format json 출력에서 result 필드(실제 모델 응답)만 추출 */
    private String extractResult(String stdout) {
        if (stdout == null || stdout.isBlank()) {
            log.warn("claude CLI 출력이 비었습니다.");
            return "";
        }
        try {
            JsonNode node = MAPPER.readTree(stdout);
            // 정상: { "type":"result", "result":"...", "is_error":false, ... }
            if (node.has("result")) {
                if (node.path("is_error").asBoolean(false)) {
                    log.error("claude CLI is_error=true. 출력: {}", stdout);
                    throw new RuntimeException("claude CLI 오류 응답");
                }
                return node.path("result").asText("").strip();
            }
            // result 필드가 없으면 원문 그대로(혹시 모를 형식 변화 대비)
            log.warn("claude CLI 출력에 result 필드 없음. 원문 사용: {}", stdout);
            return stdout;
        } catch (RuntimeException re) {
            throw re;
        } catch (Exception e) {
            // JSON이 아니면(예: 형식 변화) 원문 반환
            log.warn("claude CLI 출력 JSON 파싱 실패, 원문 사용: {}", e.getMessage());
            return stdout;
        }
    }

    private String readStream(Process process) {
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(process.getErrorStream(), StandardCharsets.UTF_8))) {
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) sb.append(line).append("\n");
            return sb.toString();
        } catch (Exception e) {
            return "(stderr 읽기 실패)";
        }
    }
}