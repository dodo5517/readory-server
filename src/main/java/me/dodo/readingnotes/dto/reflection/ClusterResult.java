package me.dodo.readingnotes.dto.reflection;

import java.util.List;

/**
 * 묶기 + 개요 결과. /reflection/cluster 응답.
 * 프론트가 이걸 들고 있다가, 사용자가 "독후감 만들기"를 누를 때 /reflection/compose로 그대로 돌려준다.
 */
public record ClusterResult(
        String tone,
        List<ClusterDto> clusters,
        String title,
        List<SectionDto> sections
) {
    public record ClusterDto(String theme, String summary, List<Integer> indices, boolean thin) {}
    public record SectionDto(String heading, List<Integer> clusterIndices) {}
}