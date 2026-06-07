package net.friendly_bets.externaldata;

import lombok.Builder;
import net.friendly_bets.models.ExpandedMatchdaySlot;

@Builder
public record ExternalMatchFetchRequest(
        String competitionCode,
        String season,
        ExpandedMatchdaySlot slot,
        String leagueId
) {
}
