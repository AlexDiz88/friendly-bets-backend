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
public class AllPlayersStatsDto {

    @Schema(description = "Список статистики всех игроков")
    private List<PlayerStatsDto> allPlayersStats;
}
