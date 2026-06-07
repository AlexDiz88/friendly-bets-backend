package net.friendly_bets.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@AllArgsConstructor
@NoArgsConstructor
@Builder
@Data
@Schema(description = "Обзор игровых недель и опционально ставки одной недели")
public class GameweeksOverviewPage {

    private List<CalendarNodeSummaryDto> calendarNodes;
    private BetsPage bets;
}
