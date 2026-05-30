package net.friendly_bets.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import net.friendly_bets.models.odds.OddsMarketGroup;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class OddsEventMarketsDto {

    private String gameResultId;
    private String homeTeamId;
    private String awayTeamId;
    private String status;
    private LocalDateTime kickoffUtc;
    @Builder.Default
    private List<String> bookmakers = new ArrayList<>();
    @Builder.Default
    private List<OddsMarketGroup> marketGroups = new ArrayList<>();
    private LocalDateTime fetchedAt;
}
