package net.friendly_bets.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import net.friendly_bets.models.odds.OddsDemoSnapshot;

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
                .mergedLineCount(snapshot.getMergedLines() != null ? snapshot.getMergedLines().size() : 0)
                .build();
    }
}
