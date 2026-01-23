package me.dodo.readingnotes.dto.user;

import me.dodo.readingnotes.domain.User;

public class ChangeUserStatusRequest {
    private User.UserStatus status;

    public User.UserStatus getStatus() {
        return status;
    }
}
