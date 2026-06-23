package me.dodo.readingnotes.service.reflection;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PreDestroy;
import me.dodo.readingnotes.domain.BookComment;
import me.dodo.readingnotes.domain.ReadingRecord;
import me.dodo.readingnotes.external.llm.LlmClient;
import me.dodo.readingnotes.repository.BookCommentRepository;
import me.dodo.readingnotes.repository.BookRepository;
import me.dodo.readingnotes.repository.ReadingRecordRepository;
import me.dodo.readingnotes.util.LooseJson;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static me.dodo.readingnotes.external.llm.LlmClient.Tier.CHEAP;
import static me.dodo.readingnotes.external.llm.LlmClient.Tier.QUALITY;

@Service
public class ReflectionService {

    private static final Logger log = LoggerFactory.getLogger(ReflectionService.class);

    private final LlmClient llmClient;
    private final ReadingRecordRepository recordRepo;
    private final BookCommentRepository bookCommentRepo;
    private final BookRepository bookRepo;
    private final ObjectMapper objectMapper;
    private final ExecutorService llmExecutor = Executors.newFixedThreadPool(2);

    public ReflectionService(LlmClient llmClient,
                             ReadingRecordRepository recordRepo,
                             BookCommentRepository bookCommentRepo,
                             BookRepository bookRepo,
                             ObjectMapper objectMapper) {
        this.llmClient = llmClient;
        this.recordRepo = recordRepo;
        this.bookCommentRepo = bookCommentRepo;
        this.bookRepo = bookRepo;
        this.objectMapper = objectMapper;
    }

    @PreDestroy
    public void shutdown() {
        llmExecutor.shutdownNow();
    }

    public void composeAsync(Long userId, Long bookId, SseEmitter emitter) {
        llmExecutor.submit(() -> {
            try {
                compose(userId, bookId, emitter);
            } catch (Exception e) {
                log.error("독후감 파이프라인 오류 userId={} bookId={}", userId, bookId, e);
                sendError(emitter, e.getMessage() != null ? e.getMessage() : "독후감 생성 중 오류가 발생했습니다.");
                try { emitter.complete(); } catch (Exception ignored) {}
            }
        });
    }

    private void compose(Long userId, Long bookId, SseEmitter emitter) throws IOException {
        var book = bookRepo.findById(bookId)
                .orElseThrow(() -> new IllegalArgumentException("책을 찾을 수 없습니다."));
        List<ReadingRecord> records = recordRepo.findAllWithCommentByUserAndBook(userId, bookId);
        if (records.isEmpty()) {
            sendError(emitter, "감상이 있는 기록이 없습니다. 먼저 기록에 감상을 남겨 주세요.");
            emitter.complete();
            return;
        }
        Optional<BookComment> bookCommentOpt = bookCommentRepo.findByUser_IdAndBook_Id(userId, bookId);

        // 1. 묶기 (Haiku)
        String clusterRaw = llmClient.complete(
                ReflectionPrompts.CLUSTER_SYSTEM,
                buildClusterUserMsg(book.getTitle(), records, bookCommentOpt),
                2000, CHEAP);
        JsonNode clusterJson = LooseJson.parse(clusterRaw);
        if (clusterJson == null) {
            sendError(emitter, "묶기 단계 응답을 파싱하지 못했습니다.");
            emitter.complete();
            return;
        }
        String tone = clusterJson.path("tone").asText();
        List<ClusterDto> clusters = parseClusters(clusterJson.path("clusters"));

        Map<String, Object> clusteredPayload = new LinkedHashMap<>();
        clusteredPayload.put("tone", tone);
        clusteredPayload.put("clusters", clusters);
        sendEvent(emitter, "clustered", clusteredPayload);

        // 2. 개요 (Haiku)
        String outlineRaw = llmClient.complete(
                ReflectionPrompts.COMPOSE_OUTLINE_SYSTEM,
                buildOutlineUserMsg(book.getTitle(), tone, clusters),
                1000, CHEAP);
        JsonNode outlineJson = LooseJson.parse(outlineRaw);
        if (outlineJson == null) {
            sendError(emitter, "개요 단계 응답을 파싱하지 못했습니다.");
            emitter.complete();
            return;
        }
        String title = outlineJson.path("title").asText();
        String outlineTone = outlineJson.path("tone").asText();
        List<SectionOutlineDto> sections = parseSections(outlineJson.path("sections"));

        Map<String, Object> outlinePayload = new LinkedHashMap<>();
        outlinePayload.put("title", title);
        outlinePayload.put("tone", outlineTone);
        outlinePayload.put("sections", sections);
        sendEvent(emitter, "outline", outlinePayload);

        // 3. 섹션별 본문 (Sonnet)
        for (SectionOutlineDto section : sections) {
            String sectionRaw = llmClient.complete(
                    ReflectionPrompts.COMPOSE_SECTION_SYSTEM,
                    buildSectionUserMsg(book.getTitle(), outlineTone, section, clusters, records),
                    4000, CHEAP);
            String body = extractSectionBody(sectionRaw);

            Map<String, String> sectionPayload = new LinkedHashMap<>();
            sectionPayload.put("heading", section.heading());
            sectionPayload.put("body", body);
            if (!sendEvent(emitter, "section", sectionPayload)) {
                log.info("클라이언트 연결 끊김 — 섹션 생성 중단 userId 기준");
                return; // 이미 끊긴 연결, 남은 섹션 생성은 헛수고
            }
        }

        Map<String, Object> donePayload = new LinkedHashMap<>();
        donePayload.put("reflectionId", null);
        sendEvent(emitter, "done", donePayload);
        emitter.complete();
    }

    // ── 메시지 빌더 ──────────────────────────────────────────────────

    private String buildClusterUserMsg(String title, List<ReadingRecord> records,
                                       Optional<BookComment> bookCommentOpt) {
        StringBuilder sb = new StringBuilder();
        sb.append("책 제목: ").append(title).append("\n\n");
        for (int i = 0; i < records.size(); i++) {
            ReadingRecord r = records.get(i);
            sb.append("[").append(i).append("] 문장: ")
              .append(r.getSentence() != null ? r.getSentence() : "(없음)")
              .append(" / 감상: ").append(r.getComment()).append("\n");
        }
        bookCommentOpt.ifPresent(bc ->
            sb.append("\n\n[자유 기록 — 특정 문장에 매인 게 아니라 책 전체에 대한 인상입니다. 이 내용은 \"전체 인상\" 묶음으로 다뤄 주세요.]\n")
              .append(bc.getContent())
        );
        return sb.toString();
    }

    private String buildOutlineUserMsg(String title, String tone, List<ClusterDto> clusters) {
        StringBuilder sb = new StringBuilder();
        sb.append("책 제목: ").append(title).append("\n");
        sb.append("전체 톤: ").append(tone).append("\n\n");
        sb.append("묶음 목록:\n");
        for (int i = 0; i < clusters.size(); i++) {
            ClusterDto c = clusters.get(i);
            sb.append("[묶음 ").append(i).append("] ")
              .append(c.theme()).append(" / ")
              .append(c.summary()).append(" / (")
              .append(c.indices().size()).append("개 기록)\n");
        }
        return sb.toString();
    }

    private String buildSectionUserMsg(String title, String tone, SectionOutlineDto section,
                                       List<ClusterDto> clusters, List<ReadingRecord> records) {
        StringBuilder sb = new StringBuilder();
        sb.append("책 제목: ").append(title).append("\n");
        sb.append("전체 톤: ").append(tone).append("\n");
        sb.append("섹션 제목: ").append(section.heading()).append("\n\n");
        sb.append("이 섹션이 다룰 묶음 재료:\n\n");

        for (int ci : section.clusterIndices()) {
            if (ci < 0 || ci >= clusters.size()) continue;
            ClusterDto cluster = clusters.get(ci);
            sb.append("감정 결: ").append(cluster.theme()).append("\n");
            sb.append("길잡이: ").append(cluster.summary()).append("\n");
            sb.append("날것 감상들:\n");

            if (cluster.indices().isEmpty()) {
                sb.append("  · (전체 인상) ").append(cluster.summary()).append("\n");
            } else {
                for (int idx : cluster.indices()) {
                    if (idx < 0 || idx >= records.size()) continue;
                    ReadingRecord r = records.get(idx);
                    sb.append("  · 문장: ").append(r.getSentence() != null ? r.getSentence() : "(없음)").append("\n");
                    sb.append("    감상: ").append(r.getComment()).append("\n");
                }
            }
            sb.append("\n");
        }

        sb.append("이 섹션에서 다룰 재료입니다. 각 항목의 \"감상\"이 독자가 실제로 쓴 말입니다. ")
          .append("이 말투와 어휘를 그대로 살려서 쓰세요. ")
          .append("독자가 쓴 표현을 그대로 두고, 끊긴 문장만 그 말투로 맺어 주세요.");

        return sb.toString();
    }

    // ── 파서 ─────────────────────────────────────────────────────────

    private List<ClusterDto> parseClusters(JsonNode node) {
        List<ClusterDto> result = new ArrayList<>();
        if (node == null || !node.isArray()) return result;
        for (JsonNode c : node) {
            String theme = c.path("theme").asText();
            String summary = c.path("summary").asText();
            boolean thin = c.path("thin").asBoolean(false);
            List<Integer> indices = new ArrayList<>();
            for (JsonNode idx : c.path("indices")) indices.add(idx.asInt());
            result.add(new ClusterDto(theme, summary, indices, thin));
        }
        return result;
    }

    private List<SectionOutlineDto> parseSections(JsonNode node) {
        List<SectionOutlineDto> result = new ArrayList<>();
        if (node == null || !node.isArray()) return result;
        for (JsonNode s : node) {
            String heading = s.path("heading").asText();
            List<Integer> clusterIndices = new ArrayList<>();
            for (JsonNode idx : s.path("clusterIndices")) clusterIndices.add(idx.asInt());
            result.add(new SectionOutlineDto(heading, clusterIndices));
        }
        return result;
    }

    private String extractSectionBody(String raw) {
        if (raw == null) return "";
        String trimmed = raw.strip();
        if (trimmed.startsWith("{")) {
            JsonNode node = LooseJson.parse(trimmed);
            if (node != null && node.has("body")) {
                return node.path("body").asText().replace("\\n", "\n");
            }
        }
        return trimmed;
    }

    // ── SSE 헬퍼 ─────────────────────────────────────────────────────

    private boolean sendEvent(SseEmitter emitter, String eventName, Object data) {
        try {
            emitter.send(SseEmitter.event().name(eventName).data(objectMapper.writeValueAsString(data)));
            return true;
        } catch (Exception e) {
            // IOException(broken pipe) 외 IllegalStateException 등도 포함 — 연결이 끊긴 것으로 본다.
            log.warn("SSE 전송 실패 event={}: {}", eventName, e.getMessage());
            return false;
        }
    }

    private void sendError(SseEmitter emitter, String message) {
        try {
            emitter.send(SseEmitter.event().name("error")
                    .data(objectMapper.writeValueAsString(Map.of("message", message))));
        } catch (IOException e) {
            log.warn("SSE 에러 전송 실패: {}", e.getMessage());
        }
    }

    // ── 내부 DTO ─────────────────────────────────────────────────────

    record ClusterDto(String theme, String summary, List<Integer> indices, boolean thin) {}
    record SectionOutlineDto(String heading, List<Integer> clusterIndices) {}
}