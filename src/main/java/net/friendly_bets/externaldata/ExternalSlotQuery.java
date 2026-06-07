package net.friendly_bets.externaldata;

import lombok.Builder;

/**
 * Provider-neutral description of how to fetch matches for a tournament slot.
 */
@Builder
public record ExternalSlotQuery(
        QueryType queryType,
        Integer matchday,
        String stage,
        Integer leg
) {
    public enum QueryType {
        MATCHDAY,
        STAGE,
        STAGE_LEG
    }
}
