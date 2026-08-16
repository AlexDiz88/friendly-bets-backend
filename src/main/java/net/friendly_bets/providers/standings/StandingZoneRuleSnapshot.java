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
    /** Optional hex color from the source (e.g. {@code #004682}), used when cssClass is provider-specific. */
    private String color;
}
