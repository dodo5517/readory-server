package me.dodo.readingnotes.service;

import jakarta.persistence.EntityNotFoundException;
import me.dodo.readingnotes.domain.ReadingRecord;
import me.dodo.readingnotes.domain.RecordHighlight;
import me.dodo.readingnotes.dto.reading.HighlightCreateRequest;
import me.dodo.readingnotes.dto.reading.HighlightItem;
import me.dodo.readingnotes.repository.ReadingRecordRepository;
import me.dodo.readingnotes.repository.RecordHighlightRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RecordHighlightServiceTest {

    @Mock ReadingRecordRepository recordRepo;
    @Mock RecordHighlightRepository highlightRepo;

    RecordHighlightService service;

    private static final Long USER_ID = 1L;
    private static final Long RECORD_ID = 10L;
    // 길이 24
    private static final String SENTENCE = "인간은 변할 수 있고, 누구나 행복해질 수 있다.";

    @BeforeEach
    void setUp() {
        service = new RecordHighlightService(recordRepo, highlightRepo);
    }

    private ReadingRecord recordWithSentence(String sentence) {
        return ReadingRecord.create(null, sentence, null, null, null, null, null);
    }

    private HighlightCreateRequest req(Integer start, Integer end, String color) {
        HighlightCreateRequest r = new HighlightCreateRequest();
        r.setStart(start);
        r.setEnd(end);
        r.setColor(color);
        return r;
    }

    @Test
    @DisplayName("유효한 범위면 하이라이트를 저장하고 항목을 반환한다")
    void add_savesAndReturns() {
        when(recordRepo.findByIdAndUserId(RECORD_ID, USER_ID))
                .thenReturn(Optional.of(recordWithSentence(SENTENCE)));
        when(highlightRepo.findByRecord_IdOrderByStartOffsetAsc(RECORD_ID)).thenReturn(List.of());
        when(highlightRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));

        HighlightItem item = service.add(USER_ID, RECORD_ID, req(0, 5, "green"));

        assertThat(item.start()).isEqualTo(0);
        assertThat(item.end()).isEqualTo(5);
        assertThat(item.color()).isEqualTo("GREEN");
        verify(highlightRepo).save(any(RecordHighlight.class));
    }

    @Test
    @DisplayName("끝 위치가 문장 길이를 넘으면 거부한다")
    void add_rejectsOutOfRange() {
        when(recordRepo.findByIdAndUserId(RECORD_ID, USER_ID))
                .thenReturn(Optional.of(recordWithSentence(SENTENCE)));

        assertThatThrownBy(() -> service.add(USER_ID, RECORD_ID, req(0, SENTENCE.length() + 1, "GREEN")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("범위");

        verify(highlightRepo, never()).save(any());
    }

    @Test
    @DisplayName("start >= end 이면 거부한다")
    void add_rejectsInvertedRange() {
        when(recordRepo.findByIdAndUserId(RECORD_ID, USER_ID))
                .thenReturn(Optional.of(recordWithSentence(SENTENCE)));

        assertThatThrownBy(() -> service.add(USER_ID, RECORD_ID, req(5, 5, "GREEN")))
                .isInstanceOf(IllegalArgumentException.class);

        verify(highlightRepo, never()).save(any());
    }

    @Test
    @DisplayName("기존 하이라이트와 겹치면 거부한다")
    void add_rejectsOverlap() {
        when(recordRepo.findByIdAndUserId(RECORD_ID, USER_ID))
                .thenReturn(Optional.of(recordWithSentence(SENTENCE)));
        ReadingRecord rec = recordWithSentence(SENTENCE);
        when(highlightRepo.findByRecord_IdOrderByStartOffsetAsc(RECORD_ID))
                .thenReturn(List.of(RecordHighlight.create(rec, 0, 5, RecordHighlight.HighlightColor.GREEN)));

        assertThatThrownBy(() -> service.add(USER_ID, RECORD_ID, req(3, 8, "PEACH")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("겹");

        verify(highlightRepo, never()).save(any());
    }

    @Test
    @DisplayName("서로 안 겹치는(인접) 범위는 허용한다")
    void add_allowsAdjacentRange() {
        when(recordRepo.findByIdAndUserId(RECORD_ID, USER_ID))
                .thenReturn(Optional.of(recordWithSentence(SENTENCE)));
        ReadingRecord rec = recordWithSentence(SENTENCE);
        when(highlightRepo.findByRecord_IdOrderByStartOffsetAsc(RECORD_ID))
                .thenReturn(List.of(RecordHighlight.create(rec, 0, 5, RecordHighlight.HighlightColor.GREEN)));
        when(highlightRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));

        HighlightItem item = service.add(USER_ID, RECORD_ID, req(5, 9, "PEACH"));

        assertThat(item.start()).isEqualTo(5);
        assertThat(item.color()).isEqualTo("PEACH");
    }

    @Test
    @DisplayName("알 수 없는 색상은 거부한다")
    void add_rejectsUnknownColor() {
        assertThatThrownBy(() -> service.add(USER_ID, RECORD_ID, req(0, 3, "BLUE")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("색상");

        verify(highlightRepo, never()).save(any());
    }

    @Test
    @DisplayName("본인 기록이 아니면 EntityNotFound를 던진다")
    void add_throwsWhenRecordMissing() {
        when(recordRepo.findByIdAndUserId(RECORD_ID, USER_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.add(USER_ID, RECORD_ID, req(0, 3, "GREEN")))
                .isInstanceOf(EntityNotFoundException.class);

        verify(highlightRepo, never()).save(any());
    }

    @Test
    @DisplayName("문장이 없는 기록엔 하이라이트할 수 없다")
    void add_rejectsWhenNoSentence() {
        when(recordRepo.findByIdAndUserId(RECORD_ID, USER_ID))
                .thenReturn(Optional.of(recordWithSentence(null)));

        assertThatThrownBy(() -> service.add(USER_ID, RECORD_ID, req(0, 3, "GREEN")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("문장");

        verify(highlightRepo, never()).save(any());
    }

    @Test
    @DisplayName("본인 하이라이트면 삭제한다")
    void delete_removesOwn() {
        ReadingRecord rec = recordWithSentence(SENTENCE);
        RecordHighlight h = RecordHighlight.create(rec, 0, 5, RecordHighlight.HighlightColor.GREEN);
        when(highlightRepo.findByIdAndRecord_User_Id(100L, USER_ID)).thenReturn(Optional.of(h));

        service.delete(USER_ID, 100L);

        verify(highlightRepo).delete(h);
    }

    @Test
    @DisplayName("본인 하이라이트가 아니면 EntityNotFound를 던진다")
    void delete_throwsWhenMissing() {
        when(highlightRepo.findByIdAndRecord_User_Id(100L, USER_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.delete(USER_ID, 100L))
                .isInstanceOf(EntityNotFoundException.class);

        verify(highlightRepo, never()).delete(any());
    }
}
