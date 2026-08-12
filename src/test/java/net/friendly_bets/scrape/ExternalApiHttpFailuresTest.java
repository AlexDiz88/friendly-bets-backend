package net.friendly_bets.scrape;

import net.friendly_bets.exceptions.ExternalApiHttpException;
import net.friendly_bets.marathonbet.client.MarathonbetHttpFetchResult;
import net.friendly_bets.marathonbet.client.MarathonbetHttpOutcome;
import net.friendly_bets.melbet.client.MelbetHttpFetchResult;
import net.friendly_bets.melbet.client.MelbetHttpOutcome;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ExternalApiHttpFailuresTest {

  @Test
  void marathonbet_parseErrorIsNotTransportFailure() {
    MarathonbetHttpFetchResult result = MarathonbetHttpFetchResult.builder()
        .success(false)
        .outcome(MarathonbetHttpOutcome.PARSE_ERROR)
        .build();
    assertFalse(ExternalApiHttpFailures.isMarathonbetTransportFailure(result));
  }

  @Test
  void marathonbet_timeoutTriggersTransportFailure() {
    MarathonbetHttpFetchResult result = MarathonbetHttpFetchResult.builder()
        .success(false)
        .outcome(MarathonbetHttpOutcome.TIMEOUT)
        .build();
    assertTrue(ExternalApiHttpFailures.isMarathonbetTransportFailure(result));
    assertThrows(ExternalApiHttpException.class, () ->
        ExternalApiHttpFailures.throwIfMarathonbetTransportFailure(result));
  }

  @Test
  void melbet_decryptErrorIsNotTransportFailure() {
    MelbetHttpFetchResult result = MelbetHttpFetchResult.builder()
        .success(false)
        .outcome(MelbetHttpOutcome.DECRYPT_ERROR)
        .build();
    assertFalse(ExternalApiHttpFailures.isMelbetTransportFailure(result));
  }
}
