package net.friendly_bets.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import net.friendly_bets.models.League;

import java.util.List;
import java.util.stream.Collectors;

@AllArgsConstructor
@NoArgsConstructor
@Builder
@Data
@Schema(description = "Лига сезона")
public class LeagueDto {

    @Schema(description = "идентификатор лиги", example = "12-байтовый хэш ID")
    private String id;

    @Schema(description = "код лиги", example = "BL")
    private String leagueCode;

    @Schema(description = "название лиги (автогенерация)", example = "АПЛ-2223")
    private String name;

    @Schema(description = "текущий игровой тур лиги", example = "14")
    private String currentMatchDay;

    @Schema(description = "список команд лиги", example = "[Team1, Team2...]")
    private List<TeamDto> teams;

    public static LeagueDto from(League league) {
        return LeagueDto.builder()
                .id(league.getId())
                .leagueCode(league.getLeagueCode().toString())
                .name(league.getName())
                .currentMatchDay(league.getCurrentMatchDay())
                .teams(TeamDto.from(league.getTeams()))
                .build();
    }

    public static List<LeagueDto> from(List<League> leagues) {
        return leagues.stream()
                .map(LeagueDto::from)
                .collect(Collectors.toList());
    }
}
