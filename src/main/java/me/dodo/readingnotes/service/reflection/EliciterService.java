package me.dodo.readingnotes.service.reflection;

import com.fasterxml.jackson.databind.JsonNode;
import me.dodo.readingnotes.domain.Book;
import me.dodo.readingnotes.domain.ReadingRecord;
import me.dodo.readingnotes.domain.User;
import me.dodo.readingnotes.dto.reflection.ElicitRequest;
import me.dodo.readingnotes.dto.reflection.ElicitResponse;
import me.dodo.readingnotes.dto.reflection.ElicitSaveRequest;
import me.dodo.readingnotes.external.llm.LlmClient;
import me.dodo.readingnotes.repository.BookRepository;
import me.dodo.readingnotes.repository.ReadingRecordRepository;
import me.dodo.readingnotes.repository.UserRepository;
import me.dodo.readingnotes.util.LooseJson;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static me.dodo.readingnotes.external.llm.LlmClient.Tier.CHEAP;

/**
 * 감상 더 끌어내기(Eliciter) 대화 서비스.
 * - 한 번의 LLM 호출(한 턴). SSE 아님(짧은 주고받기라 일반 요청/응답이 적합).
 * - stateless: 매 턴 전체 히스토리를 받아서 처리.
 * - 매듭 판정: 모델 closing OR 응답에 물음표 없음 OR 어시스턴트 6턴 초과.
 * - 대화 종료 시, 끌어낸 (질문, 감상) 쌍을 reading_record로 일괄 저장한다.
 */
@Service
public class EliciterService {

    private static final Logger log = LoggerFactory.getLogger(EliciterService.class);
    private static final int TURN_CEILING = 6;
    private static final String QUESTION_PREFIX = "(질문) ";

    private final LlmClient llmClient;
    private final ReadingRecordRepository recordRepo;
    private final BookRepository bookRepo;
    private final UserRepository userRepo;

    public EliciterService(LlmClient llmClient,
                           ReadingRecordRepository recordRepo,
                           BookRepository bookRepo,
                           UserRepository userRepo) {
        this.llmClient = llmClient;
        this.recordRepo = recordRepo;
        this.bookRepo = bookRepo;
        this.userRepo = userRepo;
    }

    public ElicitResponse talk(ElicitRequest req) {
        String userText = buildUserMessage(req);

        String raw = llmClient.complete(
                ReflectionPrompts.ELICITER_SYSTEM,
                userText,
                1200, CHEAP);

        JsonNode json = LooseJson.parse(raw);
        String reply;
        String fragment = "";
        String theme = "";
        boolean modelClosing = false;

        if (json != null) {
            reply = json.path("reply").asText("");
            fragment = json.path("fragment").asText("");
            theme = json.path("theme").asText("");
            modelClosing = json.path("closing").asBoolean(false);
        } else {
            // 파싱 실패 시 원문을 그대로 reply로(최소한 대화는 이어지게)
            reply = raw != null ? raw.strip() : "";
            log.warn("Eliciter 응답 JSON 파싱 실패, 원문 사용");
        }

        // 매듭 판정: 모델 신호 OR 질문이 아닌 평서문 OR 충분히 길어진 대화
        int assistantTurns = countAssistantTurns(req.history()) + 1; // 이번 응답 포함
        boolean hasQuestion = reply.contains("?") || reply.contains("？");
        boolean closing = modelClosing || !hasQuestion || assistantTurns >= TURN_CEILING;

        return new ElicitResponse(reply, fragment, theme, closing);
    }

    /** ELICITER_SYSTEM이 기대하는 입력: 묶음 길잡이 + 대화 내역을 하나의 텍스트로 직렬화 */
    private String buildUserMessage(ElicitRequest req) {
        StringBuilder sb = new StringBuilder();

        boolean firstTurn = req.history() == null || req.history().isEmpty();

        if (firstTurn) {
            sb.append("[대화 시작] 아래는 독자가 이미 묶어 둔 감정의 결입니다. 이걸 길잡이로 대화를 열어 주세요.\n\n");
        }

        if (req.tone() != null && !req.tone().isBlank()) {
            sb.append("전체 톤: ").append(req.tone()).append("\n");
        }
        if (req.clusters() != null && !req.clusters().isEmpty()) {
            sb.append("감정의 결(묶음):\n");
            for (ElicitRequest.ClusterInput c : req.clusters()) {
                sb.append("- ").append(c.theme());
                if (c.thin()) sb.append(" (감상이 얇음 — 보강하면 좋음)");
                sb.append(": ").append(c.summary()).append("\n");
            }
            sb.append("\n");
        }

        if (!firstTurn) {
            sb.append("지금까지의 대화:\n");
            for (ElicitRequest.Turn t : req.history()) {
                String who = "assistant".equals(t.role()) ? "결" : "독자";
                sb.append(who).append(": ").append(t.content()).append("\n");
            }
            sb.append("\n위 대화에 이어, '결'로서 다음 한 마디를 JSON으로 응답하세요.");
        } else {
            sb.append("'결'로서 첫 마디를 JSON으로 응답하세요.");
        }

        return sb.toString();
    }

    private int countAssistantTurns(List<ElicitRequest.Turn> history) {
        if (history == null) return 0;
        return (int) history.stream().filter(t -> "assistant".equals(t.role())).count();
    }

    /**
     * 대화로 끌어낸 (질문, 감상) 쌍을 reading_record로 일괄 저장한다.
     * - sentence = "(질문) " + 결의 질문  (책 문장과 구분)
     * - comment  = 사용자가 드러낸 감상(fragment)
     * - matchStatus = RESOLVED_MANUAL (이미 이 책에 확정된 기록)
     * @return 저장된 개수
     */
    @Transactional
    public int saveDrawn(Long userId, ElicitSaveRequest req) {
        if (req.pairs() == null || req.pairs().isEmpty()) return 0;

        Book book = bookRepo.findById(req.bookId())
                .orElseThrow(() -> new IllegalArgumentException("책을 찾을 수 없습니다."));
        User user = userRepo.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));

        LocalDateTime now = LocalDateTime.now();
        List<ReadingRecord> toSave = new ArrayList<>();

        for (ElicitSaveRequest.DrawnPair pair : req.pairs()) {
            if (pair.answer() == null || pair.answer().isBlank()) continue; // 감상 없는 쌍은 스킵

            ReadingRecord r = new ReadingRecord();
            r.setBook(book);
            r.setUser(user);
            String question = pair.question() != null ? pair.question().strip() : "";
            r.setSentence(truncate(QUESTION_PREFIX + question, 1000));
            r.setComment(truncate(pair.answer().strip(), 1000));
            r.setMatchStatus(ReadingRecord.MatchStatus.RESOLVED_MANUAL);
            r.setRecordedAt(now);
            r.setMatchedAt(now);
            toSave.add(r);
        }

        recordRepo.saveAll(toSave);
        log.info("대화 감상 저장 userId={} bookId={} 건수={}", userId, req.bookId(), toSave.size());
        return toSave.size();
    }

    private String truncate(String s, int max) {
        if (s == null) return null;
        return s.length() <= max ? s : s.substring(0, max);
    }
}