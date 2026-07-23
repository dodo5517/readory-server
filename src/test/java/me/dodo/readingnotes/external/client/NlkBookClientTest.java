package me.dodo.readingnotes.external.client;

import me.dodo.readingnotes.dto.book.BookCandidate;
import me.dodo.readingnotes.external.adapter.NlkBookAdapter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.hamcrest.Matchers.startsWith;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.queryParam;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class NlkBookClientTest {

    private static final String BASE = "https://www.nl.go.kr/seoji/SearchApi.do";

    private static final String ONE_DOC = "{\"TOTAL_COUNT\":\"1\",\"PAGE_NO\":\"1\",\"docs\":[{"
            + "\"TITLE\":\"죽음의 에티켓\",\"AUTHOR\":\"롤란트 슐츠\",\"PUBLISHER\":\"스노우폭스북스\","
            + "\"EA_ISBN\":\"9791188331796\",\"PUBLISH_PREDATE\":\"20190412\","
            + "\"TITLE_URL\":\"https://cover.nl.go.kr/x.jpg\",\"CONTROL_NO\":\"CN777\"}]}";

    private MockRestServiceServer server;
    private NlkBookClient client;

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder().baseUrl(BASE);
        server = MockRestServiceServer.bindTo(builder).build();
        client = new NlkBookClient(builder.build(), new NlkBookAdapter(), "TEST_KEY");
    }

    @Test
    @DisplayName("SEOJI 요청 파라미터를 구성하고 응답을 BookCandidate로 반환한다")
    void search_buildsRequestAndParsesResponse() {
        server.expect(requestTo(startsWith(BASE)))
                .andExpect(method(HttpMethod.GET))
                .andExpect(queryParam("cert_key", "TEST_KEY"))
                .andExpect(queryParam("result_style", "json"))
                .andExpect(queryParam("page_no", "1"))
                .andExpect(queryParam("page_size", "5"))
                .andRespond(withSuccess(ONE_DOC, MediaType.APPLICATION_JSON));

        List<BookCandidate> result = client.search("죽음", null, 5);

        server.verify();
        assertThat(result).hasSize(1);
        BookCandidate c = result.get(0);
        assertThat(c.getSource()).isEqualTo("NLK");
        assertThat(c.getTitle()).isEqualTo("죽음의 에티켓");
        assertThat(c.getIsbn13()).isEqualTo("9791188331796");
        assertThat(c.getExternalId()).isEqualTo("CN777");
    }

    @Test
    @DisplayName("작가가 있으면 1차 질의에 제목+작가를 함께 보낸다")
    void search_sendsTitleAndAuthorOnFirstTry() {
        server.expect(requestTo(startsWith(BASE)))
                .andExpect(queryParam("author", "Kim"))
                .andRespond(withSuccess(ONE_DOC, MediaType.APPLICATION_JSON));

        List<BookCandidate> result = client.search("죽음", "Kim 지음", 5);

        server.verify(); // 1차에서 결과가 나오면 추가 호출 없음
        assertThat(result).hasSize(1);
    }

    @Test
    @DisplayName("제목+작가가 0건이면 제목만으로 재시도한다")
    void search_fallsBackToTitleOnly() {
        server.expect(requestTo(startsWith(BASE)))
                .andExpect(queryParam("author", "Kim"))
                .andRespond(withSuccess("{\"docs\":[]}", MediaType.APPLICATION_JSON));
        server.expect(requestTo(startsWith(BASE)))
                .andExpect(request -> assertThat(request.getURI().getQuery()).doesNotContain("author="))
                .andRespond(withSuccess(ONE_DOC, MediaType.APPLICATION_JSON));

        List<BookCandidate> result = client.search("죽음", "Kim", 5);

        server.verify();
        assertThat(result).hasSize(1);
    }

    @Test
    @DisplayName("SEOJI 질의용 제목은 괄호 표기를 제거해 정규화한다")
    void search_normalizesTitleForQuery() {
        server.expect(requestTo(startsWith(BASE)))
                .andExpect(queryParam("title", "Clean"))
                .andRespond(withSuccess(ONE_DOC, MediaType.APPLICATION_JSON));

        client.search("Clean (10th Edition)", null, 5);

        server.verify();
    }

    @Test
    @DisplayName("limit은 SEOJI 허용 범위(1~50)로 정규화된다")
    void search_normalizesLimit() {
        server.expect(requestTo(startsWith(BASE)))
                .andExpect(queryParam("page_size", "50"))
                .andRespond(withSuccess("{\"docs\":[]}", MediaType.APPLICATION_JSON));

        client.search("죽음", null, 999);

        server.verify();
    }

    @Test
    @DisplayName("docs가 비면 빈 리스트를 반환한다")
    void search_emptyDocs() {
        server.expect(requestTo(startsWith(BASE)))
                .andRespond(withSuccess("{\"TOTAL_COUNT\":\"0\",\"PAGE_NO\":\"1\",\"docs\":[]}", MediaType.APPLICATION_JSON));

        assertThat(client.search("없는책", null, 10)).isEmpty();
        server.verify();
    }

    @Test
    @DisplayName("에러 응답이면 예외를 던진다")
    void search_throwsOnError() {
        server.expect(requestTo(startsWith(BASE)))
                .andRespond(withServerError());

        assertThatThrownBy(() -> client.search("죽음", null, 5))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("NLK API error");
    }
}
