package net.friendly_bets.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@AllArgsConstructor
@NoArgsConstructor
@Builder
@Data
@Schema(description = "Форма и топ-метрики игрока за сезон")
public class PlayerHighlightDto {

    private String userId;
    @Schema(description = "Последние исходы (слева старше), статусы WON/RETURNED/LOST/EMPTY")
    private List<String> recentForm;
    private BiggestWinDto biggestWin;
    private Integer bestWinStreak;
    private BestGameweekDto bestGameweek;
    private HighlightTeamDto mostProfitableTeam;
    private HighlightTeamDto mostUnprofitableTeam;
}
