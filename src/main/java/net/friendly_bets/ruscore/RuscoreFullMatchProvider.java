package net.friendly_bets.ruscore;

import lombok.RequiredArgsConstructor;
import net.friendly_bets.exceptions.BadRequestException;
import net.friendly_bets.exceptions.FullMatchNotReadyException;
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
public class RuscoreFullMatchProvider implements FullMatchProvider {

    private final RuscoreFullMatchResolver fullMatchResolver;
    private final RuscoreHttpClient httpClient;
    private final RuscoreGameSummaryParser gameSummaryParser;
    private final MatchScheduleRepository matchScheduleRepository;
    private final ExternalApiMonitoringService monitoringService;
    private final ErrorLogService errorLogService;

    @Override
    public String providerId() {
        return ExternalProviderIds.RUSCORE;
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
                ExternalProviderIds.RUSCORE,
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

        RuscoreParsedDayPage.Match resolved;
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
            errorLogService.recordFullMatchFailure(current, ExternalProviderIds.RUSCORE, e.getMessage());
            throw e;
        }

        Instant reqAt = Instant.now();
        long t0 = System.currentTimeMillis();
        String html;
        try {
            html = httpClient.fetchGameSummaryHtml(resolved.getSlug(), resolved.getEventId());
            httpLogs.add(ExternalApiMonitoringService.httpLog(
                    "GAME_SUMMARY",
                    resolved.getEventId(),
                    200,
                    "SUCCESS",
                    System.currentTimeMillis() - t0,
                    null,
                    null,
                    reqAt
            ));
        } catch (RuntimeException e) {
            httpLogs.add(ExternalApiMonitoringService.httpLog(
                    "GAME_SUMMARY",
                    resolved.getEventId(),
                    null,
                    "HTTP_ERROR",
                    System.currentTimeMillis() - t0,
                    e.getMessage(),
                    null,
                    reqAt
            ));
            monitoringService.finalizeAndSave(
                    run,
                    ExternalApiMonitoringStatus.FAILED,
                    ExternalApiMonitoringCounters.builder().requested(1).build(),
                    httpLogs,
                    List.of(current.getId()),
                    e.getMessage()
            );
            errorLogService.recordFullMatchFailure(current, ExternalProviderIds.RUSCORE, e.getMessage());
            throw e;
        }

        RuscoreParsedFullMatch parsed = gameSummaryParser.parse(html, resolved.getEventId(), resolved.getSlug());
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
        if (details.getGameScore() == null || details.getGameScore().getFullTime() == null) {
            monitoringService.finalizeAndSave(
                    run,
                    ExternalApiMonitoringStatus.FAILED,
                    ExternalApiMonitoringCounters.builder().requested(1).build(),
                    httpLogs,
                    List.of(current.getId()),
                    "ruscoreFullMatchParseFailed"
            );
            errorLogService.recordFullMatchFailure(current, ExternalProviderIds.RUSCORE, "ruscoreFullMatchParseFailed");
            throw new BadRequestException("ruscoreFullMatchParseFailed");
        }

        Instant now = Instant.now();
        FullMatchPersistSupport.apply(current, details, ExternalProviderIds.RUSCORE, now);
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

    private static FullMatchDetails toDetails(RuscoreParsedFullMatch parsed) {
        return FullMatchDetails.builder()
                .gameScore(parsed.getGameScore())
                .goals(parsed.getGoals() != null ? parsed.getGoals() : List.of())
                .stats(parsed.getStats())
                .statusText(parsed.getStatusText())
                .addedTimeFirstHalf(parsed.getAddedTimeFirstHalf())
                .addedTimeSecondHalf(parsed.getAddedTimeSecondHalf())
                .build();
    }
}
