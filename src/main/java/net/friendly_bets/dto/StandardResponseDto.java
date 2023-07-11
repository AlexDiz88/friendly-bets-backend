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
@Schema(name = "StandardResponseDto", description = "сведения о запросе")
public class StandardResponseDto {
    @Schema(description = "Текст сообщения")
    private String message;
    @Schema(description = "HTTP-статус")
    private int status;
}
