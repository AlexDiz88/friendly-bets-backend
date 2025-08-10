package net.friendly_bets.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import net.friendly_bets.models.BetTitleCategoryStats;
import net.friendly_bets.models.PlayerStatsByBetTitles;

import java.util.List;
import java.util.stream.Collectors;

@AllArgsConstructor
@NoArgsConstructor
@Builder
@Data
@Schema(description = "Статистика игрока по типам ставок")
public class PlayerStatsByBetTitlesDto {

    @Schema(description = "ID сезона", example = "12-байтовый хэш ID")
    private String seasonId;

    @Schema(description = "ID игрока", example = "12-байтовый хэш ID")
    private String userId;

    @Schema(description = "Баланс игрока", example = "125.45")
    private Double actualBalance;

    @Schema(description = "список категорий ставок со статистикой")
    private List<BetTitleCategoryStats> betTitleCategoryStats;


    public static PlayerStatsByBetTitlesDto from(PlayerStatsByBetTitles playerStatsByBetTitles) {
        return PlayerStatsByBetTitlesDto.builder()
                .seasonId(playerStatsByBetTitles.getSeasonId())
                .userId(playerStatsByBetTitles.getUserId())
                .actualBalance(playerStatsByBetTitles.getActualBalance())
                .betTitleCategoryStats(playerStatsByBetTitles.getBetTitleCategoryStats())
                .build();
    }

    public static List<PlayerStatsByBetTitlesDto> from(List<PlayerStatsByBetTitles> playerStatsByBetTitles) {
        return playerStatsByBetTitles.stream()
                .map(PlayerStatsByBetTitlesDto::from)
                .collect(Collectors.toList());
    }
}
