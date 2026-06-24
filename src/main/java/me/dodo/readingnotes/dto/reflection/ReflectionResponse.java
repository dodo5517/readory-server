package me.dodo.readingnotes.dto.reflection;

import java.time.LocalDateTime;

/** 저장된 독후감 조회 응답. */
public record ReflectionResponse(
        Long id,
        Long bookId,
        String title,
        String content,       // 마크다운
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {}