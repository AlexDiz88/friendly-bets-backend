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
@Schema(description = "Статистика игрока")
public class PlayerStatsDto {

    @Schema(description = "имя участника турнира", example = "Player")
    private String username;

    @Schema(description = "количество ставок", example = "10")
    private Integer betCount;

    @Schema(description = "количество выигранных ставок", example = "10")
    private Integer wonBetCount;

    @Schema(description = "количество вернувшихся ставок", example = "10")
    private Integer returnedBetCount;

    @Schema(description = "количество проигранных ставок", example = "10")
    private Integer lostBetCount;

    @Schema(description = "количество пустых ставок", example = "10")
    private Integer emptyBetCount;

    @Schema(description = "процеет выигранных ставок по отношению к сделанным (без учёта пустых)", example = "54.19")
    private Double winRate;

    @Schema(description = "средний коэффициент ставок (без учёта пустых)", example = "2.15")
    private Double averageOdds;

    @Schema(description = "средний коэффициент выигранных ставок (без учёта пустых)", example = "1.98")
    private Double averageWonBetOdds;

    @Schema(description = "текущий баланс игрока (может быть отрицательным)", example = "+73.32")
    private Double actualBalance;

    private Double sumOfOdds;
    private Double sumOfWonOdds;

    public double getAverageOdds() {
        return betCount > 0 ? sumOfOdds / betCount : 0.0;
    }

    public double getAverageWonOdds() {
        return wonBetCount > 0 ? sumOfWonOdds / wonBetCount : 0.0;
    }

}
