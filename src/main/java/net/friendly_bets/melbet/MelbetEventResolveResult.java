package net.friendly_bets.melbet;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public final class MelbetEventResolveResult {

    public enum MissKind {
        NO_BOOKIE_EVENT,
        MAPPING_FAILURE
    }

    private final MelbetPrematchEvent event;
    private final MissKind missKind;

    public static MelbetEventResolveResult matched(MelbetPrematchEvent event) {
        return new MelbetEventResolveResult(event, null);
    }

    public static MelbetEventResolveResult miss(MissKind missKind) {
        return new MelbetEventResolveResult(null, missKind);
    }

    public boolean isMatched() {
        return event != null;
    }
}
