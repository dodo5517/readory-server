package me.dodo.readingnotes.util;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class EbookSourceCleanerTest {

    // -----------------------------------------------------------------------
    // 교보eBook
    // -----------------------------------------------------------------------

    @Test
    @DisplayName("교보eBook - 출처 블록과 URL을 제거한다")
    void kyobo_removesSourceBlockAndUrl() {
        String input = "그들은 후세대에 대한 죄책감으로 시스템을 받아들였다.\n" +
                "\"빛의 구역\"중에서 교보eBook에서 자세히 보기:\n" +
                "https://ebook-product.kyobobook.co.kr/dig/epd/ebook/4801130650884";

        String result = EbookSourceCleaner.clean(input);

        assertThat(result).isEqualTo("그들은 후세대에 대한 죄책감으로 시스템을 받아들였다.");
        System.out.println(result);
    }

    @Test
    @DisplayName("교보eBook - URL 없이 출처 줄만 있어도 제거한다")
    void kyobo_removesSourceLineWithoutUrl() {
        String input = "본문 문장입니다.\n\"어떤 책\"중에서 교보eBook에서 자세히 보기:";

        String result = EbookSourceCleaner.clean(input);

        assertThat(result).isEqualTo("본문 문장입니다.");
        System.out.println(result);
    }

    // -----------------------------------------------------------------------
    // 네이버시리즈
    // -----------------------------------------------------------------------

    @Test
    @DisplayName("네이버시리즈 - 제목/화수/작가/출처 블록을 제거한다")
    void naver_removesMetaBlockAndSource() {
        String input = "되는데.\"\n" +
                "광마회귀 [독점]\n" +
                "95화\n" +
                "유진성\n" +
                "출처: 네이버시리즈";

        String result = EbookSourceCleaner.clean(input);

        assertThat(result).isEqualTo("되는데.\"");
        System.out.println(result);
    }

    @Test
    @DisplayName("네이버시리즈 - 출처 줄만 있어도 제거한다")
    void naver_removesSourceLineOnly() {
        String input = "문장만 있는 경우입니다.\n출처: 네이버시리즈";

        String result = EbookSourceCleaner.clean(input);

        assertThat(result).isEqualTo("문장만 있는 경우입니다.");
        System.out.println(result);
    }

    // -----------------------------------------------------------------------
    // 밀리의 서재
    // -----------------------------------------------------------------------

    @Test
    @DisplayName("밀리의 서재 - 인라인 출처와 URL을 제거한다")
    void millie_removesInlineSourceAndUrl() {
        String input = "익숙해지도록 해야 합니다. -<감정은 습관이다, 박용철 - 밀리의 서재\n" +
                "https://www.millie.co.kr/v3/bookDetail/ef6d711f9a0c45a0";

        String result = EbookSourceCleaner.clean(input);

        assertThat(result).isEqualTo("익숙해지도록 해야 합니다.");
        System.out.println(result);
    }

    @Test
    @DisplayName("밀리의 서재 - URL 없이 인라인 출처만 있어도 제거한다")
    void millie_removesInlineSourceWithoutUrl() {
        String input = "좋은 문장입니다. -<책 제목, 저자명 - 밀리의 서재";

        String result = EbookSourceCleaner.clean(input);

        assertThat(result).isEqualTo("좋은 문장입니다.");
        System.out.println(result);
    }

    // -----------------------------------------------------------------------
    // 엣지 케이스
    // -----------------------------------------------------------------------

    @Test
    @DisplayName("출처 문구가 없으면 원문을 그대로 반환한다")
    void noSource_returnsOriginal() {
        String input = "출처 없는 일반 문장입니다.";

        String result = EbookSourceCleaner.clean(input);

        assertThat(result).isEqualTo("출처 없는 일반 문장입니다.");
        System.out.println(result);
    }

    @Test
    @DisplayName("null 입력은 null을 반환한다")
    void nullInput_returnsNull() {
        assertThat(EbookSourceCleaner.clean(null)).isNull();
    }

    @Test
    @DisplayName("공백만 있는 입력은 공백을 반환한다")
    void blankInput_returnsBlank() {
        assertThat(EbookSourceCleaner.clean("   ")).isEqualTo("   ");
    }

    @Test
    @DisplayName("여러 줄 본문에서도 출처 이전 내용을 올바르게 보존한다")
    void multilineSentence_preservesBodyBeforeSource() {
        String input = "첫 번째 줄입니다.\n두 번째 줄입니다.\n\"책 제목\"중에서 교보eBook에서 자세히 보기:\nhttps://example.com";

        String result = EbookSourceCleaner.clean(input);

        assertThat(result).isEqualTo("첫 번째 줄입니다.\n두 번째 줄입니다.");
        System.out.println(result);
    }
}
