package net.friendly_bets.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.validation.constraints.Min;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class UpdatedPasswordDto {

    @Schema(description = "текущий пароль пользователя", example = "My-password1234")
    @NotNull(message = "{field.isNull}")
    @NotBlank(message = "{field.isBlank}")
    private String currentPassword;

    @Schema(description = "новый пароль пользователя", example = "My-password9999")
    @NotNull(message = "{field.isNull}")
    @NotBlank(message = "{field.isBlank}")
    @Size(min = 6, message = "{password.isShort}")
//  @Pattern(regexp = "^(?=.*[A-Z])(?=.*\\d)(?=.*[@#$%^&+=!\\-]).{6,}$", message = "{password.isWeak}")
    private String newPassword;
}
