package net.friendly_bets.scrape;

import net.friendly_bets.exceptions.ExternalApiHttpException;
import net.friendly_bets.marathonbet.client.MarathonbetHttpFetchResult;
import net.friendly_bets.marathonbet.client.MarathonbetHttpOutcome;
import net.friendly_bets.melbet.client.MelbetHttpFetchResult;
import net.friendly_bets.melbet.client.MelbetHttpOutcome;

/**
 * Classifies provider failures that should trigger primary → secondary HTTP failover.
 */
public final class ExternalApiHttpFailures {

    private ExternalApiHttpFailures() {
    }

    public static ExternalApiHttpException fetchFailed(String messageKey) {
        return new ExternalApiHttpException(messageKey);
    }

    public static boolean isHttpTransportFailure(Throwable error) {
        return error instanceof ExternalApiHttpException;
    }

    public static boolean isMarathonbetTransportFailure(MarathonbetHttpFetchResult result) {
        if (result == null || result.isSuccess()) {
            return false;
        }
        MarathonbetHttpOutcome outcome = result.getOutcome();
        return outcome == MarathonbetHttpOutcome.TIMEOUT
                || outcome == MarathonbetHttpOutcome.NETWORK_ERROR
                || outcome == MarathonbetHttpOutcome.HTTP_ERROR;
    }

    public static boolean isMelbetTransportFailure(MelbetHttpFetchResult result) {
        if (result == null || result.isSuccess()) {
            return false;
        }
        MelbetHttpOutcome outcome = result.getOutcome();
        return outcome == MelbetHttpOutcome.TIMEOUT
                || outcome == MelbetHttpOutcome.NETWORK_ERROR
                || outcome == MelbetHttpOutcome.HTTP_ERROR;
    }

    public static void throwIfMarathonbetTransportFailure(MarathonbetHttpFetchResult result) {
        if (isMarathonbetTransportFailure(result)) {
            throw fetchFailed(result.toErrorKey());
        }
    }

    public static void throwIfMelbetTransportFailure(MelbetHttpFetchResult result) {
        if (isMelbetTransportFailure(result)) {
            throw fetchFailed(result.toErrorKey());
        }
    }
}
