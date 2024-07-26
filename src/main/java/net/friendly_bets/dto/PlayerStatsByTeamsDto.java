package net.friendly_bets.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import net.friendly_bets.models.PlayerStatsByTeams;

import java.util.List;
import java.util.stream.Collectors;

@AllArgsConstructor
@NoArgsConstructor
@Builder
@Data
@Schema(description = "Статистика игрока по командам")
public class PlayerStatsByTeamsDto {

    @Schema(description = "ID сезона", example = "12-байтовый хэш ID")
    private String seasonId;

    @Schema(description = "ID лиги", example = "12-байтовый хэш ID")
    private String leagueId;

    @Schema(description = "код лиги", example = "BL")
    private String leagueCode;

    @Schema(description = "фото участника турнира", example = "путь_к_изображению.png")
    private String avatar;

    @Schema(description = "имя участника турнира", example = "Player")
    private String username;

    @Schema(description = "флаг статистики по лиге, а не по участнику")
    private boolean isLeagueStats;

    @Schema(description = "лист статистики по командам")
    private List<TeamStatsDto> teamStats;


    public static PlayerStatsByTeamsDto from(PlayerStatsByTeams playerStatsByTeams) {
        return PlayerStatsByTeamsDto.builder()
                .seasonId(playerStatsByTeams.getSeasonId())
                .leagueId(playerStatsByTeams.getLeagueId())
                .leagueCode(playerStatsByTeams.getLeagueCode())
                .avatar(playerStatsByTeams.getUser().getAvatar())
                .username(playerStatsByTeams.getUser().getUsername())
                .isLeagueStats(playerStatsByTeams.isLeagueStats())
                .teamStats(TeamStatsDto.from(playerStatsByTeams.getTeamStats()))
                .build();
    }

    public static List<PlayerStatsByTeamsDto> from(List<PlayerStatsByTeams> playerStatsByTeamsList) {
        return playerStatsByTeamsList.stream()
                .map(PlayerStatsByTeamsDto::from)
                .collect(Collectors.toList());
    }
}
