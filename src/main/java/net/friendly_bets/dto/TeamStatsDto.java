package net.friendly_bets.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import net.friendly_bets.models.Team;
import net.friendly_bets.models.TeamStats;

import java.util.List;
import java.util.stream.Collectors;

@AllArgsConstructor
@NoArgsConstructor
@Builder
@Data
@Schema(description = "Статистика команды")
public class TeamStatsDto {

    @Schema(description = "команда", example = "Арсенал")
    private Team team;

    @Schema(description = "количество обработанных ставок", example = "10")
    private Integer betCount;

    @Schema(description = "количество выигранных ставок", example = "10")
    private Integer wonBetCount;

    @Schema(description = "количество вернувшихся ставок", example = "10")
    private Integer returnedBetCount;

    @Schema(description = "количество проигранных ставок", example = "10")
    private Integer lostBetCount;

    @Schema(description = "процеет выигранных ставок по отношению к сделанным (без учёта пустых)", example = "54.19")
    private Double winRate;

    @Schema(description = "средний коэффициент ставок (без учёта пустых)", example = "2.15")
    private Double averageOdds;

    @Schema(description = "средний коэффициент выигранных ставок (без учёта пустых)", example = "1.98")
    private Double averageWonBetOdds;

    @Schema(description = "текущий баланс игрока (может быть отрицательным)", example = "-73.32")
    private Double actualBalance;

    public static TeamStatsDto from(TeamStats teamStats) {
        return TeamStatsDto.builder()
                .team(teamStats.getTeam())
                .betCount(teamStats.getBetCount())
                .wonBetCount(teamStats.getWonBetCount())
                .returnedBetCount(teamStats.getReturnedBetCount())
                .lostBetCount(teamStats.getLostBetCount())
                .winRate(teamStats.getWinRate())
                .averageOdds(teamStats.getAverageOdds())
                .averageWonBetOdds(teamStats.getAverageWonBetOdds())
                .actualBalance(teamStats.getActualBalance())
                .build();
    }

    public static List<TeamStatsDto> from(List<TeamStats> teamStatsList) {
        return teamStatsList.stream()
                .map(TeamStatsDto::from)
                .collect(Collectors.toList());
    }
}
