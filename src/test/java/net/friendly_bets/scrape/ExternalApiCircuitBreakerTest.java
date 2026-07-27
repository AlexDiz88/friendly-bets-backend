package net.friendly_bets.scrape;

import net.friendly_bets.providers.ExternalDataLayer;
import net.friendly_bets.services.ErrorLogService;
import net.friendly_bets.services.ExternalDataLayerConfigService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ExternalApiCircuitBreakerTest {

    private final AtomicInteger disableCalls = new AtomicInteger();
    private final List<ErrorLogService.Entry> logged = new ArrayList<>();

    private ExternalApiCircuitBreaker breaker;

    @BeforeEach
    void setUp() {
        ExternalDataLayerConfigService config = new ExternalDataLayerConfigService(null, null, null) {
            @Override
            public boolean disableLayer(ExternalDataLayer layer, String reason) {
                disableCalls.incrementAndGet();
                return true;
            }
        };
        ErrorLogService errors = new ErrorLogService(null) {
            @Override
            public void record(Entry entry) {
                logged.add(entry);
            }
        };
        breaker = new ExternalApiCircuitBreaker(config, errors);
        ReflectionTestUtils.setField(breaker, "failureThreshold", 3);
        disableCalls.set(0);
        logged.clear();
    }

    @Test
    void tripsAndDisablesLayerAfterThreshold() {
        breaker.recordFailure(ExternalDataLayer.SCHEDULE, "soccer365.ru", ScrapeFailureKind.TIMEOUT, "t1");
        breaker.recordFailure(ExternalDataLayer.SCHEDULE, "soccer365.ru", ScrapeFailureKind.TIMEOUT, "t2");
        assertEquals(0, disableCalls.get());

        breaker.recordFailure(ExternalDataLayer.SCHEDULE, "soccer365.ru", ScrapeFailureKind.TIMEOUT, "t3");
        assertEquals(1, disableCalls.get());
        assertEquals(1, logged.size());
        assertEquals(ExternalApiCircuitBreaker.CODE_LAYER_CIRCUIT_OPEN, logged.get(0).getCode());
    }

    @Test
    void successResetsFailureCount() {
        breaker.recordFailure(ExternalDataLayer.LIVE, "24score.pro", ScrapeFailureKind.NETWORK_ERROR, "n1");
        breaker.recordFailure(ExternalDataLayer.LIVE, "24score.pro", ScrapeFailureKind.NETWORK_ERROR, "n2");
        breaker.recordSuccess(ExternalDataLayer.LIVE);
        breaker.recordFailure(ExternalDataLayer.LIVE, "24score.pro", ScrapeFailureKind.NETWORK_ERROR, "n3");
        breaker.recordFailure(ExternalDataLayer.LIVE, "24score.pro", ScrapeFailureKind.NETWORK_ERROR, "n4");
        assertEquals(0, disableCalls.get());

        breaker.recordFailure(ExternalDataLayer.LIVE, "24score.pro", ScrapeFailureKind.NETWORK_ERROR, "n5");
        assertEquals(1, disableCalls.get());
    }

    @Test
    void parseErrorsDoNotTrip() {
        breaker.recordFailure(ExternalDataLayer.ODDS, "marathonbet", ScrapeFailureKind.PARSE_ERROR, "bad json");
        breaker.recordFailure(ExternalDataLayer.ODDS, "marathonbet", ScrapeFailureKind.PARSE_ERROR, "bad json");
        breaker.recordFailure(ExternalDataLayer.ODDS, "marathonbet", ScrapeFailureKind.PARSE_ERROR, "bad json");
        assertEquals(0, disableCalls.get());
        assertTrue(logged.isEmpty());
    }
}
