package net.friendly_bets.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import net.friendly_bets.models.odds.OddsDemoSnapshot;
import net.friendly_bets.models.odds.OddsMarketGroup;

import java.util.List;

@AllArgsConstructor
@NoArgsConstructor
@Builder
@Data
public class OddsDemoEventSummaryDto {

    private Long oddsApiEventId;
    private String home;
    private String away;
    private String eventDate;
    private String leagueSlug;
    private String status;
    private int mergedLineCount;

    public static OddsDemoEventSummaryDto from(OddsDemoSnapshot snapshot) {
        return OddsDemoEventSummaryDto.builder()
                .oddsApiEventId(snapshot.getOddsApiEventId())
                .home(snapshot.getHome())
                .away(snapshot.getAway())
                .eventDate(snapshot.getEventDate())
                .leagueSlug(snapshot.getLeagueSlug())
                .status(snapshot.getStatus())
                .mergedLineCount(countRows(snapshot))
                .build();
    }

    private static int countRows(OddsDemoSnapshot snapshot) {
        List<OddsMarketGroup> groups = snapshot.getMarketGroups();
        if (groups == null || groups.isEmpty()) {
            return 0;
        }
        return groups.stream()
                .mapToInt(g -> g.getRows() != null ? g.getRows().size() : 0)
                .sum();
    }
}
