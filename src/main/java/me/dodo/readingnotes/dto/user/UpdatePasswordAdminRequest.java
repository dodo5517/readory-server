package me.dodo.readingnotes.dto.user;

import jakarta.validation.constraints.NotBlank;

public class UpdatePasswordAdminRequest {

    @NotBlank
    private String newPassword;

    public String getNewPassword() { return newPassword; }
    public void setNewPassword(String newPassword) { this.newPassword = newPassword; }
}
