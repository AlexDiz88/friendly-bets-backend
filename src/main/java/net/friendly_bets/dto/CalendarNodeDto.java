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
@Schema(description = "Запись календаря тура")
public class CalendarNodeDto {

    @Schema(description = "идентификатор записи календаря", example = "12-байтовый хэш ID")
    private String id;

    @Schema(description = "идентификатор сезона", example = "12-байтовый хэш ID")
    private String seasonId;

    @Schema(description = "дата начала тура", example = "DD.MM.YYYY")
    private LocalDate startDate;

    @Schema(description = "дата окончания тура", example = "DD.MM.YYYY")
    private LocalDate endDate;

    @Schema(description = "список лиг и игровых дней", example = "[LeagueMatchdayNode1, LeagueMatchdayNode2...]")
    private List<LeagueMatchdayNodeDto> leagueMatchdayNodes;

    @Schema(description = "флаг, есть ли ставки в записи календаря", example = "true")
    private Boolean hasBets;

    @Schema(description = "флаг, завершен ли текущий игровой тур календаря", example = "true")
    private Boolean isFinished;

    @Schema(description = "ID предыдущей записи календаря", example = "true")
    private String previousGameweekId;

    @Schema(description = "список статистики игрового тура по участникам")
    private List<GameweekStatsDto> gameweekStats;

    public static CalendarNodeDto from(CalendarNode calendarNode, boolean isWithBets) {
        return CalendarNodeDto.builder()
                .id(calendarNode.getId())
                .seasonId(calendarNode.getSeasonId())
                .startDate(calendarNode.getStartDate())
                .endDate(calendarNode.getEndDate())
                .leagueMatchdayNodes(LeagueMatchdayNodeDto.from(calendarNode.getLeagueMatchdayNodes(), isWithBets))
                .hasBets(calendarNode.getHasBets())
                .isFinished(calendarNode.getIsFinished())
                .previousGameweekId(calendarNode.getPreviousGameweekId())
                .gameweekStats(GameweekStatsDto.from(calendarNode.getGameweekStats()))
                .build();
    }

    public static List<CalendarNodeDto> from(List<CalendarNode> calendarNodes, boolean withBets) {
        return calendarNodes.stream()
                .map(node -> CalendarNodeDto.from(node, withBets))
                .collect(Collectors.toList());
    }

}
