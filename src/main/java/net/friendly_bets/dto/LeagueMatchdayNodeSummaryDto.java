package net.friendly_bets.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import net.friendly_bets.models.League;
import net.friendly_bets.models.LeagueMatchdayNode;

import java.util.List;
import java.util.stream.Collectors;

@AllArgsConstructor
@NoArgsConstructor
@Builder
@Data
@Schema(description = "Лига и тур в записи календаря (без ставок)")
public class LeagueMatchdayNodeSummaryDto {

    private String leagueId;
    private League.LeagueCode leagueCode;
    private String matchDay;
    private Integer betCountLimit;
    private Integer defaultBetSize;

    public static LeagueMatchdayNodeSummaryDto from(LeagueMatchdayNode node) {
        return LeagueMatchdayNodeSummaryDto.builder()
                .leagueId(node.getLeagueId())
                .leagueCode(node.getLeagueCode())
                .matchDay(node.getMatchDay())
                .betCountLimit(node.getBetCountLimit())
                .defaultBetSize(node.getDefaultBetSize())
                .build();
    }

    public static List<LeagueMatchdayNodeSummaryDto> from(List<LeagueMatchdayNode> nodes) {
        if (nodes == null) {
            return List.of();
        }
        return nodes.stream()
                .map(LeagueMatchdayNodeSummaryDto::from)
                .collect(Collectors.toList());
    }
}
