package net.friendly_bets.soccer365;

import lombok.RequiredArgsConstructor;
import net.friendly_bets.exceptions.BadRequestException;
import net.friendly_bets.gameresults.MatchDataProviders;
import net.friendly_bets.models.monitoring.ExternalApiHttpLogEntry;
import net.friendly_bets.models.monitoring.ExternalApiMonitoringCounters;
import net.friendly_bets.models.monitoring.ExternalApiMonitoringRun;
import net.friendly_bets.models.monitoring.ExternalApiMonitoringStatus;
import net.friendly_bets.models.monitoring.ExternalApiMonitoringTrigger;
import net.friendly_bets.models.schedule.MatchSchedule;
import net.friendly_bets.providers.ExternalDataLayer;
import net.friendly_bets.providers.ExternalDataProvider;
import net.friendly_bets.providers.FullMatchProvider;
import net.friendly_bets.repositories.MatchScheduleRepository;
import net.friendly_bets.services.ExternalApiMonitoringService;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class Soccer365FullMatchProvider implements FullMatchProvider {

    private final Soccer365HttpClient httpClient;
    private final Soccer365GameParser gameParser;
    private final MatchScheduleRepository matchScheduleRepository;
    private final ExternalApiMonitoringService monitoringService;

    @Override
    public String providerId() {
        return MatchDataProviders.SOCCER365;
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
                MatchDataProviders.SOCCER365,
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
        String gameId = current.externalId(MatchDataProviders.sourcesStorageKey(MatchDataProviders.SOCCER365));
        if (gameId == null || gameId.isBlank()) {
            monitoringService.finalizeAndSave(
                    run,
                    ExternalApiMonitoringStatus.FAILED,
                    ExternalApiMonitoringCounters.builder().requested(1).build(),
                    httpLogs,
                    List.of(current.getId()),
                    "soccer365GameIdRequired"
            );
            throw new BadRequestException("soccer365GameIdRequired");
        }

        LocalDateTime reqAt = LocalDateTime.now();
        long t0 = System.currentTimeMillis();
        String html;
        try {
            html = httpClient.fetchGameHtml(gameId);
            httpLogs.add(ExternalApiMonitoringService.httpLog(
                    "GAME_PAGE",
                    gameId,
                    200,
                    "SUCCESS",
                    System.currentTimeMillis() - t0,
                    null,
                    null,
                    reqAt
            ));
        } catch (RuntimeException e) {
            httpLogs.add(ExternalApiMonitoringService.httpLog(
                    "GAME_PAGE",
                    gameId,
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
            throw e;
        }

        Soccer365ParsedFullMatch parsed = gameParser.parse(html);
        if (parsed.getGameScore() == null || parsed.getGameScore().getFullTime() == null) {
            monitoringService.finalizeAndSave(
                    run,
                    ExternalApiMonitoringStatus.FAILED,
                    ExternalApiMonitoringCounters.builder().requested(1).build(),
                    httpLogs,
                    List.of(current.getId()),
                    "soccer365FullMatchParseFailed"
            );
            throw new BadRequestException("soccer365FullMatchParseFailed");
        }

        Instant now = Instant.now();
        current.setGameScore(parsed.getGameScore());
        current.setGoals(parsed.getGoals());
        current.setStats(parsed.getStats());
        current.setStatus("FINISHED");
        current.setLiveMinute(null);
        current.setLiveMinuteLabel(null);
        current.setFullDetailsFetchedAt(now);
        current.setFinalizedAt(now);
        current.setFinalizedByProvider(MatchDataProviders.SOCCER365);
        current.setFetchedAt(LocalDateTime.now());
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
}
