package net.friendly_bets.marathonbet;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * Result of matching a {@code MatchSchedule} to a Marathonbet prematch event.
 */
@Getter
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public final class MarathonbetEventResolveResult {

    public enum MissKind {
        /** No prematch event in kickoff window — bookie likely has not listed the fixture yet. */
        NO_BOOKIE_EVENT,
        /** Events exist in window but sides/aliases/ambiguity prevent a unique match. */
        MAPPING_FAILURE
    }

    private final MarathonbetPrematchEvent event;
    private final MissKind missKind;

    public static MarathonbetEventResolveResult matched(MarathonbetPrematchEvent event) {
        return new MarathonbetEventResolveResult(event, null);
    }

    public static MarathonbetEventResolveResult miss(MissKind missKind) {
        return new MarathonbetEventResolveResult(null, missKind);
    }

    public boolean isMatched() {
        return event != null;
    }
}
