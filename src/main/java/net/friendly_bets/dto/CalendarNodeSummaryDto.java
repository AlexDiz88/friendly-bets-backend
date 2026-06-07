package net.friendly_bets.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import net.friendly_bets.models.CalendarNode;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

@AllArgsConstructor
@NoArgsConstructor
@Builder
@Data
@Schema(description = "Игровая неделя для списка «По турам» (без ставок)")
public class CalendarNodeSummaryDto {

    private String id;
    private String seasonId;
    private LocalDate startDate;
    private LocalDate endDate;
    private List<LeagueMatchdayNodeSummaryDto> leagueMatchdayNodes;
    private Boolean hasBets;
    private Boolean isFinished;
    private String previousGameweekId;
    private List<GameweekStatsDto> gameweekStats;

    public static CalendarNodeSummaryDto from(CalendarNode calendarNode) {
        return CalendarNodeSummaryDto.builder()
                .id(calendarNode.getId())
                .seasonId(calendarNode.getSeasonId())
                .startDate(calendarNode.getStartDate())
                .endDate(calendarNode.getEndDate())
                .leagueMatchdayNodes(LeagueMatchdayNodeSummaryDto.from(calendarNode.getLeagueMatchdayNodes()))
                .hasBets(calendarNode.getHasBets())
                .isFinished(calendarNode.getIsFinished())
                .previousGameweekId(calendarNode.getPreviousGameweekId())
                .gameweekStats(GameweekStatsDto.from(calendarNode.getGameweekStats()))
                .build();
    }

    public static List<CalendarNodeSummaryDto> from(List<CalendarNode> calendarNodes) {
        return calendarNodes.stream()
                .map(CalendarNodeSummaryDto::from)
                .collect(Collectors.toList());
    }
}
