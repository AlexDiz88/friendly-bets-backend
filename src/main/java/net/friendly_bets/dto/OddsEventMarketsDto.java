package net.friendly_bets.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import net.friendly_bets.models.odds.OddsMarketGroup;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class OddsEventMarketsDto {

    private String matchScheduleId;
    private String homeTeamId;
    private String awayTeamId;
    private String status;
    private Instant kickoffUtc;
    @Builder.Default
    private List<String> bookmakers = new ArrayList<>();
    @Builder.Default
    private List<OddsMarketGroup> marketGroups = new ArrayList<>();
    private Instant fetchedAt;
}
