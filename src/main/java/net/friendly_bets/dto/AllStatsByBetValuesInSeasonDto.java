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
@Schema(description = "Статистика всех участников сезона по диапазонам кэфов")
public class AllStatsByBetValuesInSeasonDto {

    @Schema(description = "Список статистики игроков по кэфам (по лигам и агрегат сезона)")
    private List<PlayerStatsByBetValuesDto> playersStatsByBetValues;
}
