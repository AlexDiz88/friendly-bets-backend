package net.friendly_bets.exceptions;

/**
 * FULL_MATCH source still shows an unfinished status (IN_PLAY / PAUSED / …).
 * Not a fetch failure — orchestrator defers finalize/settle; router must not failover.
 */
public class FullMatchNotReadyException extends BadRequestException {

    private final String providerStatus;

    public FullMatchNotReadyException(String providerStatus) {
        super("fullMatchNotReady");
        this.providerStatus = providerStatus;
    }

    public String getProviderStatus() {
        return providerStatus;
    }
}
