package me.dodo.readingnotes.dto.user;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import me.dodo.readingnotes.domain.User;

@Getter
@Setter
@NoArgsConstructor
public class ChangeUserStatusRequest {
    private User.UserStatus status;
}
