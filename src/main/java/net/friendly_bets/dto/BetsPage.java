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
@Schema(description = "Страница со списком всех ставок")
public class BetsPage {

    @Schema(description = "Список всех ставок")
    private List<BetDto> bets;

    @Schema(description = "Количество страниц")
    private Integer totalPages;
}
