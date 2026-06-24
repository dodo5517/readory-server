package me.dodo.readingnotes.dto.reflection;

import java.util.List;

/**
 * 감상 더 끌어내기(Eliciter) 대화 요청.
 * stateless: 매 턴 전체 대화 내역을 함께 보낸다(서버는 대화를 저장하지 않음).
 *
 * - bookId: 어떤 책에 대한 대화인지
 * - clusters: 묶기 결과(길잡이로 사용). 첫 턴에 특히 중요.
 * - tone: 전체 톤(있으면)
 * - history: 지금까지의 대화 (없거나 비어 있으면 첫 턴 → 결이 먼저 말을 연다)
 */
public record ElicitRequest(
        Long bookId,
        String tone,
        List<ClusterInput> clusters,
        List<Turn> history
) {
    public record ClusterInput(String theme, String summary, boolean thin) {}

    /** role: "user" | "assistant" */
    public record Turn(String role, String content) {}
}