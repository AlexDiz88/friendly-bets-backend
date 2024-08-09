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
public class DeletedBetDto {

    @Schema(description = "текущий пароль пользователя", example = "My-password1234")
    @NotNull(message = "{field.seasonId.isNull}")
    @NotBlank(message = "{field.seasonId.isBlank}")
    private String seasonId;

    @Schema(description = "новый пароль пользователя", example = "My-password9999")
    @NotNull(message = "{field.leagueId.isNull}")
    @NotBlank(message = "{field.leagueId.isBlank}")
    private String leagueId;

    @NotNull(message = "{field..bet.blankCalendarNodeId}")
    @NotBlank(message = "{field.bet.blankCalendarNodeId}")
    private String calendarNodeId;
}
