package net.friendly_bets.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import net.friendly_bets.models.BetValueRangeStats;
import net.friendly_bets.models.League;
import net.friendly_bets.models.PlayerStatsByBetValues;

import java.util.List;
import java.util.stream.Collectors;

@AllArgsConstructor
@NoArgsConstructor
@Builder
@Data
@Schema(description = "Статистика игрока по диапазонам кэфов")
public class PlayerStatsByBetValuesDto {

    @Schema(description = "ID сезона")
    private String seasonId;

    @Schema(description = "ID лиги или total для агрегата сезона")
    private String leagueId;

    @Schema(description = "Код лиги (EPL/BL/CL/LE); null для агрегата сезона")
    private String leagueCode;

    @Schema(description = "ID игрока")
    private String userId;

    @Schema(description = "Количество обработанных ставок")
    private Integer betCount;

    @Schema(description = "Баланс по диапазонам кэфов")
    private Double actualBalance;

    @Schema(description = "Статистика по диапазонам кэфов")
    private List<BetValueRangeStats> rangeStats;

    public static PlayerStatsByBetValuesDto from(PlayerStatsByBetValues stats) {
        League.LeagueCode leagueCode = stats.getLeagueCode();
        return PlayerStatsByBetValuesDto.builder()
                .seasonId(stats.getSeasonId())
                .leagueId(stats.getLeagueId())
                .leagueCode(leagueCode != null ? leagueCode.toString() : null)
                .userId(stats.getUserId())
                .betCount(stats.getBetCount())
                .actualBalance(stats.getActualBalance())
                .rangeStats(stats.getRangeStats())
                .build();
    }

    public static List<PlayerStatsByBetValuesDto> from(List<PlayerStatsByBetValues> stats) {
        return stats.stream()
                .map(PlayerStatsByBetValuesDto::from)
                .collect(Collectors.toList());
    }
}
