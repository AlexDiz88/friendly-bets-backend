package net.friendly_bets.providers.standings;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@NoArgsConstructor
@AllArgsConstructor
@Builder
@Data
public class StandingZoneRuleSnapshot {

    private String code;
    private String label;
    private String cssClass;
}
