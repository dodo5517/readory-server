package me.dodo.readingnotes.dto.reflection;

/**
 * 완성된 독후감 저장/수정 요청.
 * - content: 마크다운 한 덩어리(## 소제목 + 본문). 프론트가 섹션들을 합쳐 만들거나, 사용자가 수정한 내용.
 */
public record ReflectionSaveRequest(
        Long bookId,
        String title,
        String content
) {}