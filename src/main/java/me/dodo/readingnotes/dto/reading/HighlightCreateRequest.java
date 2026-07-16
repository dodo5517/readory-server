package me.dodo.readingnotes.dto.reading;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class HighlightCreateRequest {
    private Integer start;   // sentence 기준 시작(포함)
    private Integer end;     // sentence 기준 끝(미포함)
    private String color;    // "GREEN" | "PEACH"
}
