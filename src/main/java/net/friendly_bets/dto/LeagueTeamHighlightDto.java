package net.friendly_bets.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@AllArgsConstructor
@NoArgsConstructor
@Builder
@Data
@Schema(description = "Лучшая и худшая команда игрока в лиге")
public class LeagueTeamHighlightDto {

    private String leagueId;
    private String leagueCode;
    private HighlightTeamDto best;
    private HighlightTeamDto worst;
}
