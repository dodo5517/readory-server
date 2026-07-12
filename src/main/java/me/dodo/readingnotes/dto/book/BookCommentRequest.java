package me.dodo.readingnotes.dto.book;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class BookCommentRequest {
    private String content;
}
