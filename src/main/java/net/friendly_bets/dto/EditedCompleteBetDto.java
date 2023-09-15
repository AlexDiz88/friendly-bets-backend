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
public class EditedCompleteBetDto {

    @NotNull(message = "{field.seasonId.isNull}")
    @NotBlank(message = "{field.seasonId.isBlank}")
    private String seasonId;

    @NotNull(message = "{field.leagueId.isNull}")
    @NotBlank(message = "{field.leagueId.isBlank}")
    private String leagueId;

    @NotNull(message = "{field.userId.isNull}")
    @NotBlank(message = "{field.userId.isBlank}")
    private String userId;

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

    private String gameResult;

    @NotNull(message = "{field.isNull}")
    @NotBlank(message = "{field.bet.blankBetStatus}")
    private String betStatus;
}
