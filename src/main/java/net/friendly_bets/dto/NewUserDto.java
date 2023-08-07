package net.friendly_bets.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class NewUserDto {

    @Schema(description = "е-мейл пользователя", example = "example@gmail.com")
    private String email;
    @Schema(description = "пароль пользователя", example = "My-password1234")
    private String password;
}
