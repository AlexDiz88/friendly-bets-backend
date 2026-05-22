package net.friendly_bets.footballdata;

import lombok.Builder;

@Builder
public record FootballDataSlotQuery(
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
