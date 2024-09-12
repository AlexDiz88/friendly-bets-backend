package net.friendly_bets.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import net.friendly_bets.models.PlayerStats;

import java.util.Base64;
import java.util.List;
import java.util.stream.Collectors;

@AllArgsConstructor
@NoArgsConstructor
@Builder
@Data
@Schema(description = "Статистика игрока")
public class PlayerStatsDto {

    @Schema(description = "аватар пользователя в формате base64")
    private String avatar;

    @Schema(description = "имя участника турнира", example = "Player")
    private String username;

    @Schema(description = "общее количество сделанных ставок", example = "12")
    private Integer totalBets;

    @Schema(description = "количество обработанных ставок", example = "10")
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

    @Schema(description = "текущий баланс игрока (может быть отрицательным)", example = "-73.32")
    private Double actualBalance;

    public static PlayerStatsDto from(PlayerStats playerStats) {
        return PlayerStatsDto.builder()
                .avatar(playerStats.getUser().getAvatar() != null ?
                        Base64.getEncoder().encodeToString(playerStats.getUser().getAvatar().getData()) : null)
                .username(playerStats.getUser().getUsername())
                .totalBets(playerStats.getTotalBets())
                .betCount(playerStats.getBetCount())
                .wonBetCount(playerStats.getWonBetCount())
                .returnedBetCount(playerStats.getReturnedBetCount())
                .lostBetCount(playerStats.getLostBetCount())
                .emptyBetCount(playerStats.getEmptyBetCount())
                .winRate(playerStats.getWinRate())
                .averageOdds(playerStats.getAverageOdds())
                .averageWonBetOdds(playerStats.getAverageWonBetOdds())
                .actualBalance(playerStats.getActualBalance())
                .build();
    }

    public static List<PlayerStatsDto> from(List<PlayerStats> playersStatsList) {
        return playersStatsList.stream()
                .map(PlayerStatsDto::from)
                .collect(Collectors.toList());
    }
}
