package me.dodo.readingnotes.repository;

import me.dodo.readingnotes.domain.RecordHighlight;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface RecordHighlightRepository extends JpaRepository<RecordHighlight, Long> {

    // 한 기록의 하이라이트 (겹침 검증용, 시작 위치 오름차순)
    List<RecordHighlight> findByRecord_IdOrderByStartOffsetAsc(Long recordId);

    // 페이지 단위 배치 조회 (N+1 방지) — 기록 id 묶음으로 한 번에
    List<RecordHighlight> findByRecord_IdInOrderByRecord_IdAscStartOffsetAsc(List<Long> recordIds);

    // 소유권 확인 후 삭제용 (하이라이트가 속한 기록의 유저)
    Optional<RecordHighlight> findByIdAndRecord_User_Id(Long id, Long userId);
}
