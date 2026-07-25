package net.friendly_bets.soccer365;

import lombok.RequiredArgsConstructor;
import net.friendly_bets.exceptions.BadRequestException;
import net.friendly_bets.gameresults.MatchDataProviders;
import net.friendly_bets.models.schedule.MatchSchedule;
import net.friendly_bets.providers.ExternalDataLayer;
import net.friendly_bets.providers.ExternalDataProvider;
import net.friendly_bets.providers.FullMatchProvider;
import net.friendly_bets.repositories.MatchScheduleRepository;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class Soccer365FullMatchProvider implements FullMatchProvider {

    private final Soccer365HttpClient httpClient;
    private final Soccer365GameParser gameParser;
    private final MatchScheduleRepository matchScheduleRepository;

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
        if (current.getFullDetailsFetchedAt() != null) {
            return current;
        }
        String gameId = current.externalId(MatchDataProviders.sourcesStorageKey(MatchDataProviders.SOCCER365));
        if (gameId == null || gameId.isBlank()) {
            throw new BadRequestException("soccer365GameIdRequired");
        }
        String html = httpClient.fetchGameHtml(gameId);
        Soccer365ParsedFullMatch parsed = gameParser.parse(html);
        if (parsed.getGameScore() == null || parsed.getGameScore().getFullTime() == null) {
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
        return matchScheduleRepository.save(current);
    }
}
