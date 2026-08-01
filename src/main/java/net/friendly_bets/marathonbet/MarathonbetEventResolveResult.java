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
        /**
         * No uniquely matchable bookie event (not listed, or kickoff/aliases leave nothing usable).
         * Soft skip — cron will retry.
         */
        NO_BOOKIE_EVENT,
        /** Sides/aliases/ambiguity prevent a unique match while candidates exist. */
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
