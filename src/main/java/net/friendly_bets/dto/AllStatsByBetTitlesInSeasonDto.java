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
@Schema(description = "Страница со списком статистики всех типам ставок")
public class AllStatsByBetTitlesInSeasonDto {

    @Schema(description = "Список статистики всех игроков по типам ставок")
    private List<PlayerStatsByBetTitlesDto> playersStatsByBetTitles;
}
