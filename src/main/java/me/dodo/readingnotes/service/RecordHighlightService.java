package me.dodo.readingnotes.service;

import jakarta.persistence.EntityNotFoundException;
import me.dodo.readingnotes.domain.ReadingRecord;
import me.dodo.readingnotes.domain.RecordHighlight;
import me.dodo.readingnotes.dto.reading.HighlightCreateRequest;
import me.dodo.readingnotes.dto.reading.HighlightItem;
import me.dodo.readingnotes.repository.ReadingRecordRepository;
import me.dodo.readingnotes.repository.RecordHighlightRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class RecordHighlightService {

    private final ReadingRecordRepository readingRecordRepository;
    private final RecordHighlightRepository recordHighlightRepository;

    public RecordHighlightService(ReadingRecordRepository readingRecordRepository,
                                  RecordHighlightRepository recordHighlightRepository) {
        this.readingRecordRepository = readingRecordRepository;
        this.recordHighlightRepository = recordHighlightRepository;
    }

    // 하이라이트 추가 (소유권 + 범위 + 겹침 검증)
    @Transactional
    public HighlightItem add(Long userId, Long recordId, HighlightCreateRequest req) {
        if (req == null || req.getStart() == null || req.getEnd() == null || req.getColor() == null) {
            throw new IllegalArgumentException("하이라이트 범위와 색상이 필요합니다.");
        }

        RecordHighlight.HighlightColor color;
        try {
            color = RecordHighlight.HighlightColor.valueOf(req.getColor().trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("지원하지 않는 하이라이트 색상입니다: " + req.getColor());
        }

        int start = req.getStart();
        int end = req.getEnd();

        // 본인 기록만
        ReadingRecord record = readingRecordRepository.findByIdAndUserId(recordId, userId)
                .orElseThrow(() -> new EntityNotFoundException("해당 기록을 찾을 수 없습니다. recordId=" + recordId));

        String sentence = record.getSentence();
        if (sentence == null || sentence.isEmpty()) {
            throw new IllegalArgumentException("문장이 없는 기록에는 하이라이트할 수 없습니다.");
        }
        // sentence 문자열 기준 [start, end] 유효성
        if (start < 0 || end > sentence.length() || start >= end) {
            throw new IllegalArgumentException("하이라이트 범위가 올바르지 않습니다.");
        }

        // 기존 하이라이트와 겹치면 거부 (렌더 단순화)
        List<RecordHighlight> existing = recordHighlightRepository.findByRecord_IdOrderByStartOffsetAsc(recordId);
        for (RecordHighlight h : existing) {
            boolean overlaps = start < h.getEndOffset() && h.getStartOffset() < end;
            if (overlaps) {
                throw new IllegalArgumentException("이미 하이라이트된 부분과 겹칩니다.");
            }
        }

        RecordHighlight saved = recordHighlightRepository.save(
                RecordHighlight.create(record, start, end, color));
        return HighlightItem.from(saved);
    }

    // 하이라이트 삭제 (본인 것만)
    @Transactional
    public void delete(Long userId, Long highlightId) {
        RecordHighlight highlight = recordHighlightRepository.findByIdAndRecord_User_Id(highlightId, userId)
                .orElseThrow(() -> new EntityNotFoundException("해당 하이라이트를 찾을 수 없습니다. highlightId=" + highlightId));
        recordHighlightRepository.delete(highlight);
    }
}
