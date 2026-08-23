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
@Schema(description = "Лёгкий список сезонов для селекторов")
public class SeasonSummariesPage {

    @Schema(description = "Список сезонов без тяжёлых вложенных данных")
    private List<SeasonSummaryDto> seasons;
}
