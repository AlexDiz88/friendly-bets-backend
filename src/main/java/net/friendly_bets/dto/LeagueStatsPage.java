package net.friendly_bets.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import net.friendly_bets.models.League;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Schema(description = "Страница со списком статистики всех игроков")
public class LeagueStatsPage {

    @Schema(description = "Лига")
    private League league;

    @Schema(description = "Статистика игроков в лиге")
    private List<PlayerStatsDto> playersStats;
}
