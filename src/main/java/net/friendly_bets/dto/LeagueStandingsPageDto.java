package net.friendly_bets.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@NoArgsConstructor
@AllArgsConstructor
@Builder
@Data
public class LeagueStandingsPageDto {

    private String seasonId;
    private String leagueId;
    private String leagueCode;
    private String provider;
    private String sourceUrl;
    @Builder.Default
    private List<StandingZoneRuleDto> zoneRules = new ArrayList<>();
    @Builder.Default
    private List<LeagueStandingRowDto> rows = new ArrayList<>();
    private Instant updatedAt;
}
