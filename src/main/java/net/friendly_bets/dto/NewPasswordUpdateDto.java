package net.friendly_bets.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class NewPasswordUpdateDto {

    @Schema(description = "текущий пароль пользователя", example = "My-password1234")
    private String currentPassword;
    @Schema(description = "новый пароль пользователя", example = "My-password9999")
    private String newPassword;
}
