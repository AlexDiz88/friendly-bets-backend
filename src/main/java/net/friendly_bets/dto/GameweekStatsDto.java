package net.friendly_bets.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import net.friendly_bets.models.GameweekStats;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@AllArgsConstructor
@NoArgsConstructor
@Builder
@Data
@Schema(description = "Запись календаря тура")
public class GameweekStatsDto {

    @Schema(description = "флаг, завершен ли текущий игровой тур календаря", example = "true")
    private String userId;
    private Double balanceChange;
    private Double totalBalance;
    private Integer positionAfterGameweek;
    private Integer positionChange;

    public static GameweekStatsDto from(GameweekStats gameweekStats) {
        return GameweekStatsDto.builder()
                .userId(gameweekStats.getUserId())
                .balanceChange(gameweekStats.getBalanceChange())
                .totalBalance(gameweekStats.getTotalBalance())
                .positionAfterGameweek(gameweekStats.getPositionAfterGameweek())
                .positionChange(gameweekStats.getPositionChange())
                .build();
    }

    public static List<GameweekStatsDto> from(List<GameweekStats> gameweekStatsList) {
        if (gameweekStatsList == null) {
            return new ArrayList<>();
        }
        return gameweekStatsList.stream()
                .map(GameweekStatsDto::from)
                .collect(Collectors.toList());
    }

}
