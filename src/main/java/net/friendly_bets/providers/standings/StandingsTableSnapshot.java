package net.friendly_bets.providers.standings;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@NoArgsConstructor
@AllArgsConstructor
@Builder
@Data
public class StandingsTableSnapshot {

    private String sourceUrl;
    private String group;
    @Builder.Default
    private List<StandingRowSnapshot> rows = new ArrayList<>();
    @Builder.Default
    private List<StandingZoneRuleSnapshot> zoneRules = new ArrayList<>();
}
