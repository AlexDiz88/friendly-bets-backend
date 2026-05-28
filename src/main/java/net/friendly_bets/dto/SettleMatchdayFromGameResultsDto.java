package net.friendly_bets.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SettleMatchdayFromGameResultsDto {

    @NotBlank
    private String seasonId;

    @NotBlank
    private String leagueCode;

    @NotNull
    private Integer matchday;

    private String externalSeason;
}
