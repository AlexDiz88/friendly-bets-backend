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
@Schema(description = "Страница со списком статистики всех игроков по командам")
public class AllStatsByTeamsInSeasonDto {

    @Schema(description = "Список статистики всех игроков по командам")
    private List<PlayerStatsByTeamsDto> playersStatsByTeams;
}
