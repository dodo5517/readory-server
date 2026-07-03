package me.dodo.readingnotes.dto.common;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class ReissueRequest {

    @NotBlank
    private String refreshToken;

    @NotBlank
    private String deviceInfo;

    @Override // toString 예쁘게 보기 위해 오버라이딩
    public String toString() {
        return "Reissue{" +
                "refreshToken='" + refreshToken + '\'' +
                ", deviceInfo='" + deviceInfo + '\'' +
                '}';
    }
}
