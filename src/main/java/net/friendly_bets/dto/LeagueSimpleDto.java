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
@Schema(description = "Лига сезона (без списка команд)")
public class LeagueSimpleDto {

    @Schema(description = "идентификатор лиги", example = "12-байтовый хэш ID")
    private String id;

    @Schema(description = "код лиги", example = "BL")
    private String leagueCode;

    @Schema(description = "название лиги (автогенерация)", example = "АПЛ-2223")
    private String name;

    @Schema(description = "текущий игровой тур лиги", example = "14")
    private String currentMatchDay;

    public static LeagueSimpleDto from(League league) {
        return LeagueSimpleDto.builder()
                .id(league.getId())
                .leagueCode(league.getLeagueCode().toString())
                .name(league.getName())
                .currentMatchDay(league.getCurrentMatchDay())
                .build();
    }

    public static List<LeagueSimpleDto> from(List<League> leagues) {
        return leagues.stream()
                .map(LeagueSimpleDto::from)
                .collect(Collectors.toList());
    }
}
