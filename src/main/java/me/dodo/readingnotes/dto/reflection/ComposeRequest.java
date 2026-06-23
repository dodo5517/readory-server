package me.dodo.readingnotes.dto.reflection;

import java.util.List;

/**
 * 엮기 요청. 프론트가 /reflection/cluster에서 받은 묶기/개요 결과를 그대로 돌려준다.
 * 서버는 bookId로 reading_records를 다시 조회해(같은 순서) indices를 매칭한다.
 */
public record ComposeRequest(
        Long bookId,
        String tone,
        List<ClusterResult.ClusterDto> clusters,
        List<ClusterResult.SectionDto> sections
) {}