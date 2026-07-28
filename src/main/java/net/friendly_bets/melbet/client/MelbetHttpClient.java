package net.friendly_bets.melbet.client;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import net.friendly_bets.exceptions.BadRequestException;
import net.friendly_bets.melbet.config.MelbetProperties;
import net.friendly_bets.providers.ExternalDataLayer;
import net.friendly_bets.providers.ExternalProviderIds;
import net.friendly_bets.scrape.ExternalApiCircuitBreaker;
import net.friendly_bets.scrape.ScrapeFailureKind;
import net.friendly_bets.scrape.ScrapeHttpSupport;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpTimeoutException;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Optional;
import java.util.StringJoiner;

/**
 * Digitain Melbet HTTP: tournament list ({@code GetMixed…}) and full event ({@code GetEvent}).
 */
@Component
@RequiredArgsConstructor
public class MelbetHttpClient {

    private static final Duration CONNECT_TIMEOUT = Duration.ofSeconds(20);
    private static final Duration READ_TIMEOUT = Duration.ofSeconds(120);
    private static final String UA =
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/131.0.0.0 Safari/537.36";

    private final MelbetProperties properties;
    private final MelbetPayloadDecryptor decryptor;
    private final ObjectMapper objectMapper;
    private final ExternalApiCircuitBreaker circuitBreaker;
    private final HttpClient httpClient = ScrapeHttpSupport.newBrowserClient(CONNECT_TIMEOUT);

    /**
     * Prematch events with column markets (1X2 / DC / handicap / totals) for ODDS sync.
     */
    public MelbetHttpFetchResult fetchTournamentEvents(long tournamentId) {
        return fetchTournamentEvents(tournamentId, new int[]{1, 37, 2, 3}, true);
    }

    /**
     * Prematch events only ({@code stakeTypes=0}) — team names / kickoff, no market rows.
     * Does not affect the ODDS circuit breaker (admin aliases probe).
     */
    public MelbetHttpFetchResult fetchTournamentEventsForTeamNames(long tournamentId) {
        return fetchTournamentEvents(tournamentId, new int[]{0}, false);
    }

    private MelbetHttpFetchResult fetchTournamentEvents(
            long tournamentId,
            int[] stakeTypes,
            boolean affectCircuitBreaker
    ) {
        if (tournamentId <= 0) {
            throw new BadRequestException("melbetInvalidTournamentId");
        }
        StringJoiner q = new StringJoiner("&");
        q.add("period=0");
        q.add("tournamentId=" + tournamentId);
        if (stakeTypes != null) {
            for (int stakeType : stakeTypes) {
                q.add("stakeTypes=" + stakeType);
            }
        }
        q.add("isTournament=false");
        q.add("eventFilterType=false");
        q.add("includeLiveEvents=false");
        q.add("langId=" + properties.getLangId());
        q.add("partnerId=" + properties.getPartnerId());
        q.add("countryCode=");
        String path = "/" + properties.getPartnerUuid()
                + "/common/getmixedsportandeventslistwithoutright?" + q;
        return getDecrypted(path, affectCircuitBreaker);
    }

    public MelbetHttpFetchResult fetchEvent(long eventId) {
        if (eventId <= 0) {
            throw new BadRequestException("melbetInvalidEventId");
        }
        String path = "/" + properties.getPartnerUuid()
                + "/common/getevent?eventId=" + eventId
                + "&isLive=false"
                + "&langId=" + properties.getLangId()
                + "&partnerId=" + properties.getPartnerId()
                + "&countryCode=";
        return getDecrypted(path, true);
    }

    private MelbetHttpFetchResult getDecrypted(String pathAndQuery, boolean affectCircuitBreaker) {
        long started = System.nanoTime();
        String url = trimSlash(properties.getBaseUrl()) + pathAndQuery;
        HttpRequest.Builder builder = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .timeout(READ_TIMEOUT)
                .header("Accept", "application/json, text/plain, */*")
                .header("Accept-Language", "ru-RU,ru;q=0.9")
                .header("User-Agent", UA)
                .header("Referer", properties.getReferer())
                .header("Origin", "https://www.melbet.ru")
                .GET();
        try {
            HttpResponse<String> response = httpClient.send(
                    builder.build(),
                    HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8)
            );
            long durationMs = elapsedMs(started);
            Integer retryAfter = parseRetryAfter(response);
            if (response.statusCode() >= 400) {
                ScrapeFailureKind kind = ScrapeHttpSupport.classifyHttpStatus(response.statusCode());
                if (affectCircuitBreaker) {
                    circuitBreaker.recordFailure(
                            ExternalDataLayer.ODDS, ExternalProviderIds.MELBET, kind, "HTTP " + response.statusCode());
                }
                return MelbetHttpFetchResult.builder()
                        .success(false)
                        .httpStatus(response.statusCode())
                        .outcome(MelbetHttpOutcome.HTTP_ERROR)
                        .durationMs(durationMs)
                        .errorDetail(truncateBody(response.body()))
                        .retryAfterSeconds(retryAfter)
                        .build();
            }
            JsonNode root = objectMapper.readTree(response.body());
            if (root == null || !root.hasNonNull("payload")) {
                if (affectCircuitBreaker) {
                    circuitBreaker.recordFailure(
                            ExternalDataLayer.ODDS,
                            ExternalProviderIds.MELBET,
                            ScrapeFailureKind.PARSE_ERROR,
                            "missing payload");
                }
                return MelbetHttpFetchResult.builder()
                        .success(false)
                        .httpStatus(response.statusCode())
                        .outcome(MelbetHttpOutcome.PARSE_ERROR)
                        .durationMs(durationMs)
                        .errorDetail("missing payload")
                        .build();
            }
            String decrypted;
            try {
                decrypted = decryptor.decryptPayloadBase64(root.get("payload").asText());
            } catch (BadRequestException e) {
                if (affectCircuitBreaker) {
                    circuitBreaker.recordFailure(
                            ExternalDataLayer.ODDS,
                            ExternalProviderIds.MELBET,
                            ScrapeFailureKind.PARSE_ERROR,
                            e.getMessage());
                }
                return MelbetHttpFetchResult.builder()
                        .success(false)
                        .httpStatus(response.statusCode())
                        .outcome(MelbetHttpOutcome.DECRYPT_ERROR)
                        .durationMs(durationMs)
                        .errorDetail(e.getMessage())
                        .build();
            }
            JsonNode body = objectMapper.readTree(decrypted);
            if (affectCircuitBreaker) {
                circuitBreaker.recordSuccess(ExternalDataLayer.ODDS);
            }
            return MelbetHttpFetchResult.builder()
                    .success(true)
                    .httpStatus(response.statusCode())
                    .outcome(MelbetHttpOutcome.SUCCESS)
                    .durationMs(durationMs)
                    .body(body)
                    .retryAfterSeconds(retryAfter)
                    .build();
        } catch (HttpTimeoutException e) {
            if (affectCircuitBreaker) {
                circuitBreaker.recordFailure(
                        ExternalDataLayer.ODDS, ExternalProviderIds.MELBET, ScrapeFailureKind.TIMEOUT, e.getMessage());
            }
            return MelbetHttpFetchResult.builder()
                    .success(false)
                    .outcome(MelbetHttpOutcome.TIMEOUT)
                    .durationMs(elapsedMs(started))
                    .errorDetail(e.getMessage())
                    .build();
        } catch (BadRequestException e) {
            throw e;
        } catch (Exception e) {
            if (affectCircuitBreaker) {
                circuitBreaker.recordFailure(
                        ExternalDataLayer.ODDS,
                        ExternalProviderIds.MELBET,
                        ScrapeHttpSupport.classifyThrowable(e),
                        e.getMessage());
            }
            return MelbetHttpFetchResult.builder()
                    .success(false)
                    .outcome(MelbetHttpOutcome.NETWORK_ERROR)
                    .durationMs(elapsedMs(started))
                    .errorDetail(e.getMessage())
                    .build();
        }
    }

    private static String trimSlash(String base) {
        if (base == null || base.isBlank()) {
            return "https://sport.melbet.ru";
        }
        return base.endsWith("/") ? base.substring(0, base.length() - 1) : base;
    }

    private static long elapsedMs(long startedNano) {
        return (System.nanoTime() - startedNano) / 1_000_000L;
    }

    private static Integer parseRetryAfter(HttpResponse<?> response) {
        return response.headers().firstValue("retry-after")
                .flatMap(MelbetHttpClient::parsePositiveInt)
                .or(() -> response.headers().firstValue("Retry-After")
                        .flatMap(MelbetHttpClient::parsePositiveInt))
                .orElse(null);
    }

    private static Optional<Integer> parsePositiveInt(String raw) {
        if (raw == null || raw.isBlank()) {
            return Optional.empty();
        }
        try {
            int value = Integer.parseInt(raw.trim());
            return value > 0 ? Optional.of(value) : Optional.empty();
        } catch (NumberFormatException e) {
            return Optional.empty();
        }
    }

    private static String truncateBody(String body) {
        if (body == null || body.isBlank()) {
            return null;
        }
        return body.length() > 200 ? body.substring(0, 200) + "…" : body;
    }
}
