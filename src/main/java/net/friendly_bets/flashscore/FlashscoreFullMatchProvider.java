package net.friendly_bets.flashscore;

import lombok.RequiredArgsConstructor;
import net.friendly_bets.exceptions.BadRequestException;
import net.friendly_bets.exceptions.FullMatchNotReadyException;
import net.friendly_bets.flashscore.config.FlashscoreProperties;
import net.friendly_bets.models.monitoring.ExternalApiHttpLogEntry;
import net.friendly_bets.models.monitoring.ExternalApiMonitoringCounters;
import net.friendly_bets.models.monitoring.ExternalApiMonitoringRun;
import net.friendly_bets.models.monitoring.ExternalApiMonitoringStatus;
import net.friendly_bets.models.monitoring.ExternalApiMonitoringTrigger;
import net.friendly_bets.models.schedule.MatchSchedule;
import net.friendly_bets.providers.ExternalDataLayer;
import net.friendly_bets.providers.ExternalDataProvider;
import net.friendly_bets.providers.ExternalProviderIds;
import net.friendly_bets.providers.FullMatchDetails;
import net.friendly_bets.providers.FullMatchPersistSupport;
import net.friendly_bets.providers.FullMatchProvider;
import net.friendly_bets.providers.FullMatchStatusSupport;
import net.friendly_bets.repositories.MatchScheduleRepository;
import net.friendly_bets.services.ErrorLogService;
import net.friendly_bets.services.ExternalApiMonitoringService;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class FlashscoreFullMatchProvider implements FullMatchProvider {

    private final FlashscoreFullMatchResolver fullMatchResolver;
    private final FlashscoreHttpClient httpClient;
    private final FlashscoreMatchDetailParser matchDetailParser;
    private final MatchScheduleRepository matchScheduleRepository;
    private final ExternalApiMonitoringService monitoringService;
    private final ErrorLogService errorLogService;

    @Override
    public String providerId() {
        return ExternalProviderIds.FLASHSCORE;
    }

    @Override
    public Set<ExternalDataLayer> capabilities() {
        return ExternalDataProvider.of(ExternalDataLayer.FULL_MATCH);
    }

    @Override
    public MatchSchedule fetchAndPersistFullDetails(MatchSchedule match) {
        if (match == null || match.getId() == null) {
            throw new BadRequestException("matchScheduleNotFound");
        }
        MatchSchedule current = matchScheduleRepository.findById(match.getId())
                .orElseThrow(() -> new BadRequestException("matchScheduleNotFound"));

        ExternalApiMonitoringRun run = monitoringService.begin(
                ExternalDataLayer.FULL_MATCH,
                ExternalProviderIds.FLASHSCORE,
                ExternalApiMonitoringTrigger.ORCHESTRATOR,
                current.getLeagueCode(),
                current.getSeasonId()
        );
        run.setMatchday(current.getMatchday());
        List<ExternalApiHttpLogEntry> httpLogs = new ArrayList<>();

        if (current.getFullDetailsFetchedAt() != null) {
            monitoringService.finalizeAndSave(
                    run,
                    ExternalApiMonitoringStatus.SKIPPED,
                    ExternalApiMonitoringCounters.builder().requested(1).skipped(1).saved(0).build(),
                    httpLogs,
                    List.of(),
                    "alreadyFetched"
            );
            return current;
        }

        if (current.getUtcKickoff() == null) {
            monitoringService.finalizeAndSave(
                    run,
                    ExternalApiMonitoringStatus.SKIPPED,
                    ExternalApiMonitoringCounters.builder().requested(1).skipped(1).saved(0).build(),
                    httpLogs,
                    List.of(),
                    ExternalApiMonitoringService.reasonMissingUtcKickoff(1)
            );
            return current;
        }

        FlashscoreParsedDayPage.Match resolved;
        try {
            resolved = fullMatchResolver.resolveMatch(current);
        } catch (BadRequestException e) {
            monitoringService.finalizeAndSave(
                    run,
                    ExternalApiMonitoringStatus.FAILED,
                    ExternalApiMonitoringCounters.builder().requested(1).build(),
                    httpLogs,
                    List.of(current.getId()),
                    e.getMessage()
            );
            errorLogService.recordFullMatchFailure(current, ExternalProviderIds.FLASHSCORE, e.getMessage());
            throw e;
        }

        String eventId = resolved.getEventId();
        FlashscoreParsedFullMatch parsed;
        try {
            parsed = fetchAndParseMatch(eventId, resolved, httpLogs);
        } catch (RuntimeException e) {
            String message = e instanceof BadRequestException bad ? bad.getMessage() : "flashscoreFetchFailed";
            monitoringService.finalizeAndSave(
                    run,
                    ExternalApiMonitoringStatus.FAILED,
                    ExternalApiMonitoringCounters.builder().requested(1).build(),
                    httpLogs,
                    List.of(current.getId()),
                    message
            );
            errorLogService.recordFullMatchFailure(current, ExternalProviderIds.FLASHSCORE, message);
            throw e;
        }

        FullMatchDetails details = toDetails(parsed);
        if (!FullMatchStatusSupport.isProviderFinished(details.getStatusText())) {
            monitoringService.finalizeAndSave(
                    run,
                    ExternalApiMonitoringStatus.SKIPPED,
                    ExternalApiMonitoringCounters.builder().requested(1).skipped(1).saved(0).build(),
                    httpLogs,
                    List.of(),
                    "fullMatchNotReady"
            );
            throw new FullMatchNotReadyException(details.getStatusText());
        }
        if (details.getGameScore() == null
                || details.getGameScore().getFullTime() == null
                || details.getGameScore().getFirstTime() == null) {
            monitoringService.finalizeAndSave(
                    run,
                    ExternalApiMonitoringStatus.FAILED,
                    ExternalApiMonitoringCounters.builder().requested(1).build(),
                    httpLogs,
                    List.of(current.getId()),
                    "flashscoreFullMatchParseFailed"
            );
            errorLogService.recordFullMatchFailure(current, ExternalProviderIds.FLASHSCORE, "flashscoreFullMatchParseFailed");
            throw new BadRequestException("flashscoreFullMatchParseFailed");
        }

        Instant now = Instant.now();
        FullMatchPersistSupport.apply(current, details, ExternalProviderIds.FLASHSCORE, now);
        MatchSchedule saved = matchScheduleRepository.save(current);

        monitoringService.finalizeAndSave(
                run,
                ExternalApiMonitoringStatus.SUCCESS,
                ExternalApiMonitoringCounters.builder().requested(1).saved(1).build(),
                httpLogs,
                List.of(),
                null
        );
        return saved;
    }

    public FlashscoreParsedFullMatch fetchAndParseMatch(
            String eventId,
            FlashscoreParsedDayPage.Match resolved,
            List<ExternalApiHttpLogEntry> httpLogs
    ) {
        Instant reqAt = Instant.now();
        long t0 = System.currentTimeMillis();
        String summary;
        try {
            summary = httpClient.fetchMatchSummaryFeed(eventId);
            httpLogs.add(logEntry("MATCH_SUMMARY", eventId, t0, reqAt));
        } catch (RuntimeException e) {
            httpLogs.add(logError("MATCH_SUMMARY", eventId, t0, reqAt, e));
            throw e;
        }
        t0 = System.currentTimeMillis();
        reqAt = Instant.now();
        String stats;
        try {
            stats = httpClient.fetchMatchStatsFeed(eventId);
            httpLogs.add(logEntry("MATCH_STATS", eventId, t0, reqAt));
        } catch (RuntimeException e) {
            httpLogs.add(logError("MATCH_STATS", eventId, t0, reqAt, e));
            throw e;
        }
        t0 = System.currentTimeMillis();
        reqAt = Instant.now();
        String result;
        try {
            result = httpClient.fetchMatchResultFeed(eventId);
            httpLogs.add(logEntry("MATCH_RESULT", eventId, t0, reqAt));
        } catch (RuntimeException e) {
            httpLogs.add(logError("MATCH_RESULT", eventId, t0, reqAt, e));
            throw e;
        }
        t0 = System.currentTimeMillis();
        reqAt = Instant.now();
        String h2h = null;
        try {
            h2h = httpClient.fetchMatchH2HFeed(eventId);
            httpLogs.add(logEntry("MATCH_H2H", eventId, t0, reqAt));
        } catch (RuntimeException e) {
            httpLogs.add(logError("MATCH_H2H", eventId, t0, reqAt, e));
        }
        FlashscoreParsedFullMatch parsed = matchDetailParser.parse(summary, stats, result, eventId, h2h);
        String homeName = resolved.getHomeName();
        String awayName = resolved.getAwayName();
        if (parsed.getHomeTeamName() != null && !parsed.getHomeTeamName().isBlank()) {
            homeName = parsed.getHomeTeamName();
        }
        if (parsed.getAwayTeamName() != null && !parsed.getAwayTeamName().isBlank()) {
            awayName = parsed.getAwayTeamName();
        }
        return FlashscoreParsedFullMatch.builder()
                .eventId(parsed.getEventId())
                .statusText(parsed.getStatusText())
                .homeTeamName(homeName)
                .awayTeamName(awayName)
                .competitionName(parsed.getCompetitionName())
                .gameScore(parsed.getGameScore())
                .goals(parsed.getGoals())
                .stats(parsed.getStats())
                .addedTimeFirstHalf(parsed.getAddedTimeFirstHalf())
                .addedTimeSecondHalf(parsed.getAddedTimeSecondHalf())
                .build();
    }

    private static FullMatchDetails toDetails(FlashscoreParsedFullMatch parsed) {
        return FullMatchDetails.builder()
                .gameScore(parsed.getGameScore())
                .goals(parsed.getGoals() != null ? parsed.getGoals() : List.of())
                .stats(parsed.getStats())
                .statusText(parsed.getStatusText())
                .addedTimeFirstHalf(parsed.getAddedTimeFirstHalf())
                .addedTimeSecondHalf(parsed.getAddedTimeSecondHalf())
                .build();
    }

    private static ExternalApiHttpLogEntry logEntry(String type, String eventId, long t0, Instant reqAt) {
        return ExternalApiMonitoringService.httpLog(
                type,
                eventId,
                200,
                "SUCCESS",
                System.currentTimeMillis() - t0,
                null,
                null,
                reqAt
        );
    }

    private static ExternalApiHttpLogEntry logError(String type, String eventId, long t0, Instant reqAt, RuntimeException e) {
        return ExternalApiMonitoringService.httpLog(
                type,
                eventId,
                null,
                "HTTP_ERROR",
                System.currentTimeMillis() - t0,
                e.getMessage(),
                null,
                reqAt
        );
    }
}
