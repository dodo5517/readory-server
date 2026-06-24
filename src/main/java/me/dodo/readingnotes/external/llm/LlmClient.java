package me.dodo.readingnotes.external.llm;

/**
 * LLM 호출 추상화. 시스템 프롬프트 + 유저 메시지를 받아 텍스트 응답을 반환한다.
 * API 키는 구현체에서만 다루며, 프론트로 절대 노출되지 않는다.
 */
public interface LlmClient {

    /** 사용할 모델 등급 */
    enum Tier {
        CHEAP,   // 묶기, 개요, 질의응답 (Haiku)
        QUALITY  // 섹션 엮기 (Sonnet)
    }

    /**
     * @param system     시스템 프롬프트 (prompt caching 대상)
     * @param userText   유저 메시지 본문
     * @param maxTokens  최대 출력 토큰
     * @param tier       모델 등급
     * @return 모델이 생성한 텍스트
     */
    String complete(String system, String userText, int maxTokens, Tier tier);
}
