package net.friendly_bets.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Schema(description = "Страница со списком статистики по командам")
public class StatsByTeamsDto {

    @Schema(description = "Статистика по командам")
    private PlayerStatsByTeamsDto statsByTeams;
}
