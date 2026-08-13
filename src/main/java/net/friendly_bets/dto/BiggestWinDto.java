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
@Schema(description = "Самый крупный выигрыш игрока")
public class BiggestWinDto {

    private Double balanceChange;
    private Double betOdds;
    private Integer betSize;
    private String leagueCode;
    private String matchDay;
    private String calendarNodeId;
    private HighlightTeamDto homeTeam;
    private HighlightTeamDto awayTeam;
}
