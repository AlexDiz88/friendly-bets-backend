package net.friendly_bets.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Schema(description = "Количество ставок участников по матчам слота")
public class SlotMatchBetCountsDto {

    @Schema(description = "Ключ — homeTeamId_awayTeamId, значение — число размещённых ставок")
    private Map<String, Integer> counts;
}
