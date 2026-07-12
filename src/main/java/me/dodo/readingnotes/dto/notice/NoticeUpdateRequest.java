package me.dodo.readingnotes.dto.notice;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class NoticeUpdateRequest {
    private String message;
    private Boolean enabled;
}
