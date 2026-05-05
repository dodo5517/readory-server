package me.dodo.readingnotes.service;

import me.dodo.readingnotes.domain.ReadingRecord;
import me.dodo.readingnotes.dto.reading.SentenceCleanProjection;
import me.dodo.readingnotes.repository.ReadingRecordRepository;
import me.dodo.readingnotes.util.EbookSourceCleaner;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class CleanBatchService {

    private final ReadingRecordRepository readingRecordRepository;

    @Autowired
    public CleanBatchService(ReadingRecordRepository readingRecordRepository) {
        this.readingRecordRepository = readingRecordRepository;
    }

    // 배치 1회마다 트랜잭션 종료 — OOM 방지를 위해 ReadingRecordService에서 분리
    @Transactional
    public int[] cleanBatch(int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        List<SentenceCleanProjection> batch = readingRecordRepository.findAllForClean(pageable).getContent();
        int updated = 0;

        for (SentenceCleanProjection proj : batch) {
            String base = proj.getSentenceOriginal() != null
                    ? proj.getSentenceOriginal()
                    : proj.getSentence();
            String cleaned = EbookSourceCleaner.clean(base);
            if (!cleaned.equals(proj.getSentence())) {
                ReadingRecord record = readingRecordRepository.findById(proj.getId()).orElse(null);
                if (record == null) continue;
                if (record.getSentenceOriginal() == null) {
                    record.setSentenceOriginal(proj.getSentence());
                }
                record.setSentence(cleaned);
                readingRecordRepository.save(record);
                updated++;
            }
        }
        return new int[]{batch.size(), updated};
    }
}
