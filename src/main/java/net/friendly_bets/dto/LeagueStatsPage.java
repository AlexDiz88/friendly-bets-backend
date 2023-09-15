package net.friendly_bets.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Schema(description = "Страница со списком статистики всех игроков")
public class LeagueStatsPage {

    @Schema(description = "Лига (без списка команд и ставок)")
    private SimpleLeagueDto simpleLeague;

    @Schema(description = "Статистика игроков в лиге")
    private List<PlayerStatsDto> playersStats;
}
