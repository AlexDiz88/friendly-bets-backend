package net.friendly_bets.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ResetPasswordDto {

    @Schema(description = "одноразовый токен из письма")
    @NotNull(message = "{field.isNull}")
    @NotBlank(message = "{field.isBlank}")
    private String token;

    @Schema(description = "новый пароль")
    @NotNull(message = "{field.isNull}")
    @NotBlank(message = "{field.isBlank}")
    @Size(min = 6, message = "{password.isShort}")
    private String password;

    @Schema(description = "повтор нового пароля")
    @NotNull(message = "{field.isNull}")
    @NotBlank(message = "{field.isBlank}")
    @Size(min = 6, message = "{password.isShort}")
    private String passwordRepeat;
}
