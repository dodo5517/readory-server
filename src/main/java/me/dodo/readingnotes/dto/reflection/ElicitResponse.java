package me.dodo.readingnotes.dto.reflection;

/**
 * Eliciter 한 턴의 응답.
 * - reply: 사용자에게 보여줄 '결'의 말
 * - fragment: 이번 턴에 새로 드러난 감상 요약(없으면 빈 문자열)
 * - theme: 그 감상이 가까운 감정 결(없으면 빈 문자열)
 * - closing: 지금이 매듭 자리인지(true면 프론트가 "더 얘기/정리" 선택지 표시)
 */
public record ElicitResponse(
        String reply,
        String fragment,
        String theme,
        boolean closing
) {}