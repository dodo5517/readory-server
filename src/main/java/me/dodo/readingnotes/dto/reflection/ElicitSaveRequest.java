package me.dodo.readingnotes.dto.reflection;

import java.util.List;

/**
 * 대화로 끌어낸 감상을 reading_record로 일괄 저장하는 요청.
 * 대화를 마치고 "정리하기"를 누를 때 프론트가 수집한 쌍들을 보낸다.
 *
 * 각 쌍:
 * - question: 직전 '결'의 질문 (sentence에 "(질문) " 접두어를 붙여 저장)
 * - answer:   그 턴에 드러난 감상(fragment) → comment로 저장
 */
public record ElicitSaveRequest(
        Long bookId,
        List<DrawnPair> pairs
) {
    public record DrawnPair(String question, String answer) {}
}