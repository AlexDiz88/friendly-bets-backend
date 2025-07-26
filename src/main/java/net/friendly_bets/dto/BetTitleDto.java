package net.friendly_bets.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import net.friendly_bets.models.BetTitle;

import java.util.List;
import java.util.stream.Collectors;

@AllArgsConstructor
@NoArgsConstructor
@Builder
@Data
@Schema(description = "Наименование ставки")
public class BetTitleDto {

    @Schema(description = "числовой код ставки", example = "1234")
    private short code;

    @Schema(description = "название ставки (на русском)", example = "П1 + ТМ 2,5")
    private String label;

    @Schema(description = "флаг негативной ставки", example = "Обе забьют - нет (тогда флаг = true)")
    private boolean isNot;

    public static BetTitleDto from(BetTitle betTitle) {
        return BetTitleDto.builder()
                .label(betTitle.getLabel())
                .isNot(betTitle.isNot())
                .code(betTitle.getCode())
                .build();
    }

    public static List<BetTitleDto> from(List<BetTitle> betTitles) {
        return betTitles.stream()
                .map(BetTitleDto::from)
                .collect(Collectors.toList());
    }
}
