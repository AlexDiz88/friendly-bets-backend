package net.friendly_bets.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class UpdatedThemeSettingsDto {

    @Schema(description = "предпочитаемая тема: light, dark или system", example = "system")
    @NotNull(message = "{field.isNull}")
    @NotBlank(message = "{field.isBlank}")
    private String themePreference;

    @Schema(description = "показывать переключатель темы в шапке")
    @NotNull(message = "{field.isNull}")
    private Boolean showThemeToggle;
}
