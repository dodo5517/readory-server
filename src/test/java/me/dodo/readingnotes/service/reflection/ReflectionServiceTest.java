package me.dodo.readingnotes.service.reflection;

import com.fasterxml.jackson.databind.ObjectMapper;
import me.dodo.readingnotes.domain.Book;
import me.dodo.readingnotes.domain.BookComment;
import me.dodo.readingnotes.domain.ReadingRecord;
import me.dodo.readingnotes.dto.reflection.ClusterResult;
import me.dodo.readingnotes.external.llm.LlmClient;
import me.dodo.readingnotes.repository.BookCommentRepository;
import me.dodo.readingnotes.repository.BookRepository;
import me.dodo.readingnotes.repository.ReadingRecordRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static me.dodo.readingnotes.external.llm.LlmClient.Tier.CHEAP;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReflectionServiceTest {

    @Mock LlmClient llmClient;
    @Mock ReadingRecordRepository recordRepo;
    @Mock BookCommentRepository bookCommentRepo;
    @Mock BookRepository bookRepo;

    ReflectionService service;

    private static final Long USER_ID = 1L;
    private static final Long BOOK_ID = 10L;

    @BeforeEach
    void setUp() {
        // ObjectMapper는 실제 객체 사용 (SSE 직렬화에만 쓰이며 clusterOnly엔 무관)
        service = new ReflectionService(llmClient, recordRepo, bookCommentRepo, bookRepo, new ObjectMapper());
    }

    private Book book(String title) {
        Book b = new Book();
        b.setTitle(title);
        return b;
    }

    private ReadingRecord record(String sentence, String comment) {
        return ReadingRecord.create(null, sentence, null, comment, null, null, null);
    }

    // 묶기 단계의 정상 JSON 응답
    private String clusterJson() {
        return """
                {
                  "tone": "차분함",
                  "clusters": [
                    {"theme": "상실", "summary": "떠나보냄에 대한 감상", "indices": [0, 1], "thin": false},
                    {"theme": "회복", "summary": "다시 일어섬", "indices": [2], "thin": true}
                  ]
                }
                """;
    }

    // 개요 단계의 정상 JSON 응답
    private String outlineJson(String tone) {
        return """
                {
                  "title": "흔들리며 나아가기",
                  "tone": "%s",
                  "sections": [
                    {"heading": "떠나보냄", "clusterIndices": [0]},
                    {"heading": "다시", "clusterIndices": [1]}
                  ]
                }
                """.formatted(tone);
    }

    @Test
    @DisplayName("묶기→개요 2단계를 거쳐 ClusterResult를 조립한다")
    void clusterOnly_buildsResultFromTwoStages() {
        when(bookRepo.findById(BOOK_ID)).thenReturn(Optional.of(book("어떤 책")));
        when(recordRepo.findAllWithCommentByUserAndBook(USER_ID, BOOK_ID))
                .thenReturn(List.of(record("문장A", "감상A"), record("문장B", "감상B"), record("문장C", "감상C")));
        when(bookCommentRepo.findByUser_IdAndBook_Id(USER_ID, BOOK_ID)).thenReturn(Optional.empty());
        // 첫 complete=묶기, 두 번째=개요 (둘 다 CHEAP)
        when(llmClient.complete(any(), any(), anyInt(), eq(CHEAP)))
                .thenReturn(clusterJson(), outlineJson("따뜻함"));

        ClusterResult result = service.clusterOnly(USER_ID, BOOK_ID);

        assertThat(result.title()).isEqualTo("흔들리며 나아가기");
        assertThat(result.clusters()).hasSize(2);
        assertThat(result.clusters().get(0).theme()).isEqualTo("상실");
        assertThat(result.clusters().get(0).indices()).containsExactly(0, 1);
        assertThat(result.clusters().get(1).thin()).isTrue();
        assertThat(result.sections()).hasSize(2);
        assertThat(result.sections().get(0).heading()).isEqualTo("떠나보냄");
        assertThat(result.sections().get(0).clusterIndices()).containsExactly(0);
    }

    @Test
    @DisplayName("개요가 톤을 다시 정하면 그 톤을 우선한다")
    void clusterOnly_outlineTonePrecedence() {
        when(bookRepo.findById(BOOK_ID)).thenReturn(Optional.of(book("책")));
        when(recordRepo.findAllWithCommentByUserAndBook(USER_ID, BOOK_ID))
                .thenReturn(List.of(record("s", "c")));
        when(bookCommentRepo.findByUser_IdAndBook_Id(USER_ID, BOOK_ID)).thenReturn(Optional.empty());
        // 묶기 톤="차분함", 개요 톤="따뜻함" → 개요 톤 우선
        when(llmClient.complete(any(), any(), anyInt(), eq(CHEAP)))
                .thenReturn(clusterJson(), outlineJson("따뜻함"));

        ClusterResult result = service.clusterOnly(USER_ID, BOOK_ID);

        assertThat(result.tone()).isEqualTo("따뜻함");
    }

    @Test
    @DisplayName("개요가 톤을 비워두면 묶기 단계의 톤을 유지한다")
    void clusterOnly_fallsBackToClusterTone() {
        when(bookRepo.findById(BOOK_ID)).thenReturn(Optional.of(book("책")));
        when(recordRepo.findAllWithCommentByUserAndBook(USER_ID, BOOK_ID))
                .thenReturn(List.of(record("s", "c")));
        when(bookCommentRepo.findByUser_IdAndBook_Id(USER_ID, BOOK_ID)).thenReturn(Optional.empty());
        // 개요 톤을 빈 문자열로 → 묶기 톤("차분함") 유지
        when(llmClient.complete(any(), any(), anyInt(), eq(CHEAP)))
                .thenReturn(clusterJson(), outlineJson(""));

        ClusterResult result = service.clusterOnly(USER_ID, BOOK_ID);

        assertThat(result.tone()).isEqualTo("차분함");
    }

    @Test
    @DisplayName("두 단계 모두 CHEAP(Haiku) 등급으로 호출한다")
    void clusterOnly_usesCheapTierForBothStages() {
        when(bookRepo.findById(BOOK_ID)).thenReturn(Optional.of(book("책")));
        when(recordRepo.findAllWithCommentByUserAndBook(USER_ID, BOOK_ID))
                .thenReturn(List.of(record("s", "c")));
        when(bookCommentRepo.findByUser_IdAndBook_Id(USER_ID, BOOK_ID)).thenReturn(Optional.empty());
        when(llmClient.complete(any(), any(), anyInt(), eq(CHEAP)))
                .thenReturn(clusterJson(), outlineJson("따뜻함"));

        service.clusterOnly(USER_ID, BOOK_ID);

        // 묶기 + 개요 = CHEAP 2회, QUALITY는 0회
        verify(llmClient, times(2)).complete(any(), any(), anyInt(), eq(CHEAP));
        verify(llmClient, never()).complete(any(), any(), anyInt(), eq(LlmClient.Tier.QUALITY));
    }

    @Test
    @DisplayName("책이 없으면 예외를 던지고 LLM을 호출하지 않는다")
    void clusterOnly_throwsWhenBookMissing() {
        when(bookRepo.findById(BOOK_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.clusterOnly(USER_ID, BOOK_ID))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("책");

        verify(llmClient, never()).complete(any(), any(), anyInt(), any());
    }

    @Test
    @DisplayName("감상이 있는 기록이 하나도 없으면 예외를 던지고 LLM을 호출하지 않는다")
    void clusterOnly_throwsWhenNoRecords() {
        when(bookRepo.findById(BOOK_ID)).thenReturn(Optional.of(book("책")));
        when(recordRepo.findAllWithCommentByUserAndBook(USER_ID, BOOK_ID)).thenReturn(List.of());

        assertThatThrownBy(() -> service.clusterOnly(USER_ID, BOOK_ID))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("감상");

        verify(llmClient, never()).complete(any(), any(), anyInt(), any());
    }

    @Test
    @DisplayName("묶기 응답이 파싱 불가능하면 IllegalStateException을 던지고 개요 단계로 넘어가지 않는다")
    void clusterOnly_throwsWhenClusterUnparseable() {
        when(bookRepo.findById(BOOK_ID)).thenReturn(Optional.of(book("책")));
        when(recordRepo.findAllWithCommentByUserAndBook(USER_ID, BOOK_ID))
                .thenReturn(List.of(record("s", "c")));
        when(bookCommentRepo.findByUser_IdAndBook_Id(USER_ID, BOOK_ID)).thenReturn(Optional.empty());
        // 묶기 응답이 JSON 객체가 아님 → LooseJson.parse == null
        when(llmClient.complete(any(), any(), anyInt(), eq(CHEAP)))
                .thenReturn("죄송하지만 처리할 수 없습니다");

        assertThatThrownBy(() -> service.clusterOnly(USER_ID, BOOK_ID))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("묶기");

        // 묶기에서 멈췄으므로 complete는 단 1회만 호출됨 (개요 미진입)
        verify(llmClient, times(1)).complete(any(), any(), anyInt(), eq(CHEAP));
    }

    @Test
    @DisplayName("개요 응답이 파싱 불가능하면 IllegalStateException을 던진다")
    void clusterOnly_throwsWhenOutlineUnparseable() {
        when(bookRepo.findById(BOOK_ID)).thenReturn(Optional.of(book("책")));
        when(recordRepo.findAllWithCommentByUserAndBook(USER_ID, BOOK_ID))
                .thenReturn(List.of(record("s", "c")));
        when(bookCommentRepo.findByUser_IdAndBook_Id(USER_ID, BOOK_ID)).thenReturn(Optional.empty());
        when(llmClient.complete(any(), any(), anyInt(), eq(CHEAP)))
                .thenReturn(clusterJson(), "JSON이 아닌 응답");

        assertThatThrownBy(() -> service.clusterOnly(USER_ID, BOOK_ID))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("개요");
    }

    @Test
    @DisplayName("자유 기록(BookComment)이 있으면 묶기 프롬프트에 \"전체 인상\" 안내와 함께 포함된다")
    void clusterOnly_includesBookCommentInClusterPrompt() {
        when(bookRepo.findById(BOOK_ID)).thenReturn(Optional.of(book("책")));
        when(recordRepo.findAllWithCommentByUserAndBook(USER_ID, BOOK_ID))
                .thenReturn(List.of(record("문장", "감상")));
        BookComment bc = new BookComment();
        bc.setContent("이 책은 내 인생을 바꿨다");
        when(bookCommentRepo.findByUser_IdAndBook_Id(USER_ID, BOOK_ID)).thenReturn(Optional.of(bc));
        when(llmClient.complete(any(), any(), anyInt(), eq(CHEAP)))
                .thenReturn(clusterJson(), outlineJson("따뜻함"));

        service.clusterOnly(USER_ID, BOOK_ID);

        // 첫 호출(묶기)의 userText에 자유 기록 내용이 담겼는지 검증
        ArgumentCaptor<String> userTextCaptor = ArgumentCaptor.forClass(String.class);
        verify(llmClient, times(2)).complete(any(), userTextCaptor.capture(), anyInt(), eq(CHEAP));
        String clusterUserMsg = userTextCaptor.getAllValues().get(0);
        assertThat(clusterUserMsg).contains("이 책은 내 인생을 바꿨다");
        assertThat(clusterUserMsg).contains("전체 인상");
    }
}