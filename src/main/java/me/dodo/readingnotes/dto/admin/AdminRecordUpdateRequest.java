package me.dodo.readingnotes.dto.admin;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class AdminRecordUpdateRequest {
    private String rawTitle;
    private String rawAuthor;
    private String sentence;
    private String comment;
}
