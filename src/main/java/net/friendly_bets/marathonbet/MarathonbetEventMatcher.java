package net.friendly_bets.marathonbet;

import lombok.RequiredArgsConstructor;
import net.friendly_bets.gameresults.ApiSyncIssueService;
import net.friendly_bets.marathonbet.config.MarathonbetProperties;
import net.friendly_bets.models.Team;
import net.friendly_bets.models.gameresults.GameResultRecord;
import net.friendly_bets.repositories.GameResultRecordRepository;
import net.friendly_bets.services.TeamAliasResolver;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class MarathonbetEventMatcher {

    private final TeamAliasResolver teamAliasResolver;
    private final ApiSyncIssueService apiSyncIssueService;
    private final GameResultRecordRepository gameResultRecordRepository;
    private final MarathonbetProperties properties;

    public Optional<MarathonbetPrematchEvent> resolveAndPersistTreeId(
            GameResultRecord match,
            List<MarathonbetPrematchEvent> tournamentEvents,
            String leagueCode,
            String season,
            int matchday
    ) {
        Optional<MarathonbetPrematchEvent> resolved = resolve(match, tournamentEvents);
        if (resolved.isEmpty() && match != null && match.getId() != null) {
            List<MarathonbetPrematchEvent> candidates = filterByKickoffWindow(match, tournamentEvents);
            List<MarathonbetPrematchEvent> matched = new ArrayList<>();
            for (MarathonbetPrematchEvent event : candidates) {
                if (sidesMatch(match, event)) {
                    matched.add(event);
                }
            }
            if (matched.size() > 1) {
                Optional<MarathonbetPrematchEvent> disambiguated = pickClosestKickoff(match, matched);
                if (disambiguated.isPresent()) {
                    return disambiguated;
                }
                apiSyncIssueService.recordMarathonbetEventMappingMissing(
                        match, leagueCode, season, matchday, "ambiguousMarathonbetEventMatch");
            } else {
                apiSyncIssueService.recordMarathonbetEventMappingMissing(
                        match, leagueCode, season, matchday, null);
            }
            return Optional.empty();
        }
        if (resolved.isEmpty()) {
            return Optional.empty();
        }
        if (match != null && match.getId() != null) {
            long treeId = resolved.get().getTreeId();
            if (match.getMarathonbetTreeId() == null || match.getMarathonbetTreeId() != treeId) {
                match.setMarathonbetTreeId(treeId);
                gameResultRecordRepository.save(match);
            }
        }
        return resolved;
    }

    public Optional<MarathonbetPrematchEvent> resolve(
            GameResultRecord match,
            List<MarathonbetPrematchEvent> tournamentEvents
    ) {
        if (match == null) {
            return Optional.empty();
        }
        if (match.getMarathonbetTreeId() != null && match.getMarathonbetTreeId() > 0) {
            return tournamentEvents.stream()
                    .filter(e -> e.getTreeId() == match.getMarathonbetTreeId())
                    .findFirst();
        }

        List<MarathonbetPrematchEvent> candidates = filterByKickoffWindow(match, tournamentEvents);
        List<MarathonbetPrematchEvent> matched = new ArrayList<>();
        for (MarathonbetPrematchEvent event : candidates) {
            if (sidesMatch(match, event)) {
                matched.add(event);
            }
        }
        if (matched.size() == 1) {
            return Optional.of(matched.get(0));
        }
        if (matched.size() > 1) {
            return pickClosestKickoff(match, matched);
        }
        return Optional.empty();
    }

    public List<MarathonbetPrematchEvent> eventsForPendingMatches(
            List<GameResultRecord> pending,
            List<MarathonbetPrematchEvent> tournamentEvents
    ) {
        if (pending == null || pending.isEmpty() || tournamentEvents == null || tournamentEvents.isEmpty()) {
            return List.of();
        }
        LinkedHashSet<Long> seen = new LinkedHashSet<>();
        List<MarathonbetPrematchEvent> result = new ArrayList<>();
        for (GameResultRecord match : pending) {
            for (MarathonbetPrematchEvent event : filterByKickoffWindow(match, tournamentEvents)) {
                if (seen.add(event.getTreeId())) {
                    result.add(event);
                }
            }
        }
        return result;
    }

    private List<MarathonbetPrematchEvent> filterByKickoffWindow(
            GameResultRecord match,
            List<MarathonbetPrematchEvent> events
    ) {
        if (match.getUtcDate() == null || events == null) {
            return List.of();
        }
        long center = match.getUtcDate().toInstant(java.time.ZoneOffset.UTC).toEpochMilli();
        long windowMs = properties.getEventWindowHours() * 3_600_000L;
        List<MarathonbetPrematchEvent> filtered = new ArrayList<>();
        for (MarathonbetPrematchEvent event : events) {
            Long eventKickoff = event.getDisplayTimeMillis();
            if (eventKickoff != null && Math.abs(eventKickoff - center) <= windowMs) {
                filtered.add(event);
            }
        }
        return filtered;
    }

    private Optional<MarathonbetPrematchEvent> pickClosestKickoff(
            GameResultRecord match,
            List<MarathonbetPrematchEvent> matched
    ) {
        if (match.getUtcDate() == null) {
            return Optional.empty();
        }
        long center = match.getUtcDate().toInstant(java.time.ZoneOffset.UTC).toEpochMilli();
        MarathonbetPrematchEvent best = null;
        long bestDelta = Long.MAX_VALUE;
        long secondBestDelta = Long.MAX_VALUE;
        for (MarathonbetPrematchEvent event : matched) {
            Long eventKickoff = event.getDisplayTimeMillis();
            if (eventKickoff == null) {
                continue;
            }
            long delta = Math.abs(eventKickoff - center);
            if (delta < bestDelta) {
                secondBestDelta = bestDelta;
                bestDelta = delta;
                best = event;
            } else if (delta < secondBestDelta) {
                secondBestDelta = delta;
            }
        }
        if (best == null) {
            return Optional.empty();
        }
        // Ambiguous if two events are within 1h of each other relative to match kickoff.
        if (secondBestDelta == Long.MAX_VALUE || bestDelta + 3_600_000 <= secondBestDelta) {
            return Optional.of(best);
        }
        return Optional.empty();
    }

    private boolean sidesMatch(GameResultRecord match, MarathonbetPrematchEvent event) {
        return sideMatches(match, event, true) && sideMatches(match, event, false);
    }

    /**
     * Only truth source: scraped Marathonbet name ↔ {@code external_aliases} provider {@code marathonbet}.
     */
    private boolean sideMatches(GameResultRecord match, MarathonbetPrematchEvent event, boolean home) {
        String marathonName = home ? event.getHomeTeam() : event.getAwayTeam();
        String teamId = home ? match.getHomeTeamId() : match.getAwayTeamId();
        if (teamId == null || teamId.isBlank() || marathonName == null || marathonName.isBlank()) {
            return false;
        }
        Optional<Team> byAlias = teamAliasResolver.resolveMarathonbetByName(marathonName);
        return byAlias.isPresent() && teamId.equals(byAlias.get().getId());
    }
}
