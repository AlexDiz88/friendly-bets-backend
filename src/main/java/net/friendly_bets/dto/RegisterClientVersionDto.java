package net.friendly_bets.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class RegisterClientVersionDto {

    @NotNull(message = "{field.isNull}")
    @NotBlank(message = "{field.isBlank}")
    @Pattern(regexp = "\\d+", message = "invalidClientBuildId")
    private String buildId;
}
