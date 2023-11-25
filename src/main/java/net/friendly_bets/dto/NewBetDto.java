package net.friendly_bets.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.validation.constraints.Min;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class NewBetDto {

    @NotNull(message = "{field.isNull}")
    @NotBlank(message = "{field.isBlank}")
    private String userId;

    @NotNull(message = "{field.isNull}")
    @NotBlank(message = "{field.isBlank}")
    private String seasonId;

    @NotNull(message = "{field.isNull}")
    @NotBlank(message = "{field.isBlank}")
    private String leagueId;

    @NotNull(message = "{field.isNull}")
    @NotBlank(message = "{field.bet.blankMatchDay}")
    private String matchDay;

    private String gameId;

    private LocalDateTime gameDate;

    @NotNull(message = "{field.isNull}")
    @NotBlank(message = "{field.bet.blankHomeTeam}")
    private String homeTeamId;

    @NotNull(message = "{field.isNull}")
    @NotBlank(message = "{field.bet.blankAwayTeam}")
    private String awayTeamId;

    @NotNull(message = "{field.isNull}")
    @NotBlank(message = "{field.bet.blankBetTitle}")
    private String betTitle;

    @NotNull(message = "{field.bet.betOddsIsNull}")
    private Double betOdds;

    @NotNull(message = "{field.bet.betSizeIsNull}")
    @Min(value = 1, message = "{field.bet.betSizeMinValue}")
    private Integer betSize;
}
