package me.dodo.readingnotes.util;

import java.util.regex.Pattern;

public class EbookSourceCleaner {

    private EbookSourceCleaner() {}

    // -----------------------------------------------------------------------
    // 교보eBook: "「...」중에서 교보eBook에서 자세히 보기:" 로 시작하는 줄
    // -----------------------------------------------------------------------
    // "제목"중에서 줄
    private static final Pattern KYOBO_TITLE_LINE = Pattern.compile(
            "\\n[^\\n]*중에서\\s*$",
            Pattern.MULTILINE
    );

    // 빈 줄 + "교보eBook에서 자세히 보기:" + URL
    private static final Pattern KYOBO_SOURCE_BLOCK = Pattern.compile(
            "\\n*교보eBook에서 자세히 보기:[^\\n]*(?:\\nhttps?://\\S+)?",
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
        result = KYOBO_SOURCE_BLOCK.matcher(result).replaceAll("");
        result = KYOBO_TITLE_LINE.matcher(result).replaceAll("");
        result = NAVER_BLOCK.matcher(result).replaceAll("");
        result = MILLIE_INLINE.matcher(result).replaceAll("");
        result = TRAILING_URL.matcher(result).replaceAll("");

        return result.strip();
    }
}