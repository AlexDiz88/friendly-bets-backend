package net.friendly_bets.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import net.friendly_bets.models.odds.MergedOddsLine;
import net.friendly_bets.models.odds.OddsDemoSnapshot;

import java.time.LocalDateTime;
import java.util.List;

@AllArgsConstructor
@NoArgsConstructor
@Builder
@Data
public class OddsDemoEventDetailDto {

    private Long oddsApiEventId;
    private String home;
    private String away;
    private String eventDate;
    private String leagueSlug;
    private String status;
    private List<String> bookmakers;
    private List<MergedOddsLine> mergedLines;
    private LocalDateTime fetchedAt;

    public static OddsDemoEventDetailDto from(OddsDemoSnapshot snapshot) {
        return OddsDemoEventDetailDto.builder()
                .oddsApiEventId(snapshot.getOddsApiEventId())
                .home(snapshot.getHome())
                .away(snapshot.getAway())
                .eventDate(snapshot.getEventDate())
                .leagueSlug(snapshot.getLeagueSlug())
                .status(snapshot.getStatus())
                .bookmakers(snapshot.getBookmakers())
                .mergedLines(snapshot.getMergedLines())
                .fetchedAt(snapshot.getFetchedAt())
                .build();
    }
}
