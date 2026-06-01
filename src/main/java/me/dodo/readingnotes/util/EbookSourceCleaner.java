package me.dodo.readingnotes.util;

import java.util.regex.Pattern;

public class EbookSourceCleaner {

    private EbookSourceCleaner() {}

    // -----------------------------------------------------------------------
    // 교보eBook 멀티라인: "[제목]중에서" 줄 바로 다음에 "교보eBook에서 자세히 보기:" 줄이
    //   이어지는 형식 + 선택적 URL 줄
    // -----------------------------------------------------------------------
    private static final Pattern KYOBO_MULTILINE_BLOCK = Pattern.compile(
            "\\n[^\\n]*중에서\\s*\\n교보eBook에서 자세히 보기:[^\\n]*(?:\\nhttps?://\\S+)?",
            Pattern.MULTILINE
    );

    // -----------------------------------------------------------------------
    // 네이버시리즈: "출처: 네이버시리즈" 줄과, 그 앞 제목/화수/작가 블록
    //   - 본문과 출처 블록 사이의 경계를 찾기 위해
    //     "출처: 네이버시리즈" 직전의 연속된 짧은 줄 3개까지 함께 제거
    // -----------------------------------------------------------------------
    private static final Pattern NAVER_BLOCK = Pattern.compile(
            "\\n(?:[^\\n]*\\n){0,3}출처: 네이버시리즈\\s*$",
            Pattern.MULTILINE
    );

    // -----------------------------------------------------------------------
    // 밀리의 서재: " -{제목}, {저자} - 밀리의 서재" 인라인 패턴 + 다음 줄 URL
    //   공백·하이픈·열림꺾쇠 조합으로 시작하는 출처 구분자를 기준으로 제거
    // -----------------------------------------------------------------------
    private static final Pattern MILLIE_INLINE = Pattern.compile(
            "\\s*-[<]?[^\\n]+-\\s*밀리의 서재(?:\\nhttps?://\\S+)?\\s*$",
            Pattern.MULTILINE
    );

    // -----------------------------------------------------------------------
    // 교보eBook 인라인: "[책제목] 중에서 교보eBook에서 자세히 보기 : URL" 이
    //   본문과 같은 줄에 이어지는 신규 형식 (콜론 앞 공백, URL 동일 줄)
    // 예시: "...흙맛 센스로 유명했다. 감 두 사람의 인터내셔널 New Face Book 중에서 교보eBook에서 자세히 보기 : https://..."
    // [^\n.!?。]* 로 마침표 앞까지만 매치해 본문이 잘리는 것을 방지
    // -----------------------------------------------------------------------
    private static final Pattern KYOBO_INLINE_BLOCK = Pattern.compile(
            "[^\\n.!?。]*중에서\\s+교보eBook에서 자세히 보기\\s*:[^\\n]*$",
            Pattern.MULTILINE
    );

    // -----------------------------------------------------------------------
    // 공통 URL 후행 줄 제거 (플랫폼 판별 후 남은 URL 처리용 안전망)
    // -----------------------------------------------------------------------
    private static final Pattern TRAILING_URL = Pattern.compile(
            "\\nhttps?://\\S+\\s*$",
            Pattern.MULTILINE
    );

    /**
     * 입력된 sentence에서 ebook 출처 문구를 제거하고 정제된 문장을 반환한다.
     * 출처 문구가 없으면 입력값을 그대로 반환한다.
     */
    public static String clean(String sentence) {
        if (sentence == null || sentence.isBlank()) {
            return sentence;
        }

        String result = sentence;
        result = KYOBO_INLINE_BLOCK.matcher(result).replaceAll("");
        result = KYOBO_MULTILINE_BLOCK.matcher(result).replaceAll("");
        result = NAVER_BLOCK.matcher(result).replaceAll("");
        result = MILLIE_INLINE.matcher(result).replaceAll("");
        result = TRAILING_URL.matcher(result).replaceAll("");

        return result.strip();
    }
}