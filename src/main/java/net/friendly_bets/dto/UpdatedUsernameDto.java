package net.friendly_bets.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.validation.constraints.*;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class UpdatedUsernameDto {

    @Schema(description = "новое имя пользователя", example = "BestPlayer777")
    @NotNull(message = "{field.isNull}")
    @NotBlank(message = "{field.isBlank}")
    @Size(min = 2, message = "{username.isShort}")
    @Pattern(regexp = "^[A-Za-zА-Яа-яЁё]+[A-Za-z0-9А-Яа-яЁё\\s\\-_]*$", message = "{username.invalid}")
    private String newUsername;
}
