package me.dodo.readingnotes.external.adapter;

import com.fasterxml.jackson.databind.ObjectMapper;
import me.dodo.readingnotes.dto.book.BookCandidate;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class NlkBookAdapterTest {

    private final NlkBookAdapter adapter = new NlkBookAdapter();
    private final ObjectMapper mapper = new ObjectMapper();

    private NlkBookAdapter.NlkResponse parse(String json) throws Exception {
        return mapper.readValue(json, NlkBookAdapter.NlkResponse.class);
    }

    @Test
    @DisplayName("SEOJI 응답 필드를 BookCandidate로 매핑한다")
    void adapt_mapsFields() throws Exception {
        String json = "{"
                + "\"TOTAL_COUNT\":\"1\",\"PAGE_NO\":\"1\","
                + "\"docs\":[{"
                + "\"TITLE\":\"아주 편안한 죽음\","
                + "\"AUTHOR\":\"시몬 드 보부아르 지음, 강초롱 옮김\","
                + "\"PUBLISHER\":\"을유문화사\","
                + "\"EA_ISBN\":\"9788932474151\","
                + "\"SET_ISBN\":\"\","
                + "\"PUBLISH_PREDATE\":\"20211125\","
                + "\"TITLE_URL\":\"https://cover.nl.go.kr/abc.jpg\","
                + "\"CONTROL_NO\":\"CN12345\","
                + "\"KDC\":\"860\""
                + "}]}";

        List<BookCandidate> result = adapter.adapt(parse(json));

        assertThat(result).hasSize(1);
        BookCandidate c = result.get(0);
        assertThat(c.getSource()).isEqualTo("NLK");
        assertThat(c.getTitle()).isEqualTo("아주 편안한 죽음");
        assertThat(c.getAuthor()).isEqualTo("시몬 드 보부아르 지음, 강초롱 옮김");
        assertThat(c.getPublisher()).isEqualTo("을유문화사");
        assertThat(c.getIsbn13()).isEqualTo("9788932474151");
        assertThat(c.getIsbn10()).isEqualTo(""); // SEOJI는 ISBN10 미제공
        assertThat(c.getPublishedDate()).isEqualTo(LocalDate.of(2021, 11, 25));
        assertThat(c.getThumbnailUrl()).isEqualTo("https://cover.nl.go.kr/abc.jpg");
        assertThat(c.getExternalId()).isEqualTo("CN12345");
    }

    @Test
    @DisplayName("제어번호가 없으면 externalId는 ISBN13으로 대체된다")
    void adapt_externalIdFallsBackToIsbn13() throws Exception {
        String json = "{\"docs\":[{\"TITLE\":\"제목\",\"EA_ISBN\":\"9791234567890\"}]}";

        List<BookCandidate> result = adapter.adapt(parse(json));

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getExternalId()).isEqualTo("9791234567890");
    }

    @Test
    @DisplayName("제목이 없는 문서는 후보에서 제외한다")
    void adapt_skipsBlankTitle() throws Exception {
        String json = "{\"docs\":["
                + "{\"TITLE\":\"  \",\"EA_ISBN\":\"9788900000001\"},"
                + "{\"TITLE\":\"정상 제목\",\"EA_ISBN\":\"9788900000002\"}]}";

        List<BookCandidate> result = adapter.adapt(parse(json));

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getTitle()).isEqualTo("정상 제목");
    }

    @Test
    @DisplayName("잘못된 형식의 발행일은 null로 처리한다")
    void adapt_invalidDateBecomesNull() throws Exception {
        String json = "{\"docs\":[{\"TITLE\":\"제목\",\"PUBLISH_PREDATE\":\"2021\"}]}";

        List<BookCandidate> result = adapter.adapt(parse(json));

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getPublishedDate()).isNull();
    }

    @Test
    @DisplayName("docs가 없거나 응답이 null이면 빈 리스트를 반환한다")
    void adapt_emptyWhenNoDocs() throws Exception {
        assertThat(adapter.adapt(parse("{\"TOTAL_COUNT\":\"0\",\"PAGE_NO\":\"1\"}"))).isEmpty();
        assertThat(adapter.adapt(null)).isEmpty();
    }

    @Test
    @DisplayName("getSource는 NLK를 반환한다")
    void getSource_returnsNlk() {
        assertThat(adapter.getSource()).isEqualTo("NLK");
    }
}
