package net.friendly_bets.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.List;

@AllArgsConstructor
@NoArgsConstructor
@Builder
@Data
@Schema(description = "Лучший тур игрока")
public class BestGameweekDto {

    private String calendarNodeId;
    private LocalDate startDate;
    private LocalDate endDate;
    private Double balanceChange;
    private List<HighlightMatchdayDto> matchdays;
}
