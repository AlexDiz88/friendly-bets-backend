package net.friendly_bets.marathonbet;

import lombok.RequiredArgsConstructor;
import net.friendly_bets.marathonbet.config.MarathonbetProperties;
import net.friendly_bets.models.Team;
import net.friendly_bets.models.odds.Odds;
import net.friendly_bets.models.schedule.MatchSchedule;
import net.friendly_bets.repositories.OddsRepository;
import net.friendly_bets.services.ErrorLogService;
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
    private final ErrorLogService errorLogService;
    private final OddsRepository oddsRepository;
    private final MarathonbetProperties properties;

    public MarathonbetEventResolveResult resolveAndRecordMappingIssue(
            MatchSchedule match,
            List<MarathonbetPrematchEvent> tournamentEvents,
            String leagueCode,
            String season,
            int matchday
    ) {
        Optional<MarathonbetPrematchEvent> resolved = resolve(match, tournamentEvents);
        if (resolved.isPresent()) {
            return MarathonbetEventResolveResult.matched(resolved.get());
        }
        if (match == null || match.getId() == null) {
            return MarathonbetEventResolveResult.miss(MarathonbetEventResolveResult.MissKind.MAPPING_FAILURE);
        }
        if (match.getUtcKickoff() == null) {
            errorLogService.recordEventMappingMissing(
                    match, "marathonbet", leagueCode, season, matchday, "matchKickoffMissing");
            return MarathonbetEventResolveResult.miss(MarathonbetEventResolveResult.MissKind.MAPPING_FAILURE);
        }

        List<MarathonbetPrematchEvent> candidates = filterByKickoffWindow(match, tournamentEvents);
        if (candidates.isEmpty()) {
            // Soft: fixture not on bookie tournament page yet (e.g. next matchday).
            return MarathonbetEventResolveResult.miss(MarathonbetEventResolveResult.MissKind.NO_BOOKIE_EVENT);
        }

        List<MarathonbetPrematchEvent> matched = new ArrayList<>();
        for (MarathonbetPrematchEvent event : candidates) {
            if (sidesMatch(match, event)) {
                matched.add(event);
            }
        }
        if (matched.size() > 1) {
            Optional<MarathonbetPrematchEvent> disambiguated = pickClosestKickoff(match, matched);
            if (disambiguated.isPresent()) {
                return MarathonbetEventResolveResult.matched(disambiguated.get());
            }
            errorLogService.recordEventMappingMissing(
                    match, "marathonbet", leagueCode, season, matchday, "ambiguousMarathonbetEventMatch");
            return MarathonbetEventResolveResult.miss(MarathonbetEventResolveResult.MissKind.MAPPING_FAILURE);
        }

        errorLogService.recordEventMappingMissing(
                match, "marathonbet", leagueCode, season, matchday, null);
        return MarathonbetEventResolveResult.miss(MarathonbetEventResolveResult.MissKind.MAPPING_FAILURE);
    }

    public Optional<MarathonbetPrematchEvent> resolve(
            MatchSchedule match,
            List<MarathonbetPrematchEvent> tournamentEvents
    ) {
        if (match == null) {
            return Optional.empty();
        }
        Long cachedTreeId = resolveCachedTreeId(match.getId());
        if (cachedTreeId != null && cachedTreeId > 0) {
            Optional<MarathonbetPrematchEvent> byTreeId = tournamentEvents.stream()
                    .filter(e -> e.getTreeId() == cachedTreeId)
                    .findFirst();
            if (byTreeId.isPresent()) {
                return byTreeId;
            }
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
            List<MatchSchedule> pending,
            List<MarathonbetPrematchEvent> tournamentEvents
    ) {
        if (pending == null || pending.isEmpty() || tournamentEvents == null || tournamentEvents.isEmpty()) {
            return List.of();
        }
        LinkedHashSet<Long> seen = new LinkedHashSet<>();
        List<MarathonbetPrematchEvent> result = new ArrayList<>();
        for (MatchSchedule match : pending) {
            for (MarathonbetPrematchEvent event : filterByKickoffWindow(match, tournamentEvents)) {
                if (seen.add(event.getTreeId())) {
                    result.add(event);
                }
            }
        }
        return result;
    }

    private Long resolveCachedTreeId(String matchScheduleId) {
        if (matchScheduleId == null || matchScheduleId.isBlank()) {
            return null;
        }
        return oddsRepository.findByMatchScheduleId(matchScheduleId)
                .map(Odds::getMarathonbetTreeId)
                .orElse(null);
    }

    private List<MarathonbetPrematchEvent> filterByKickoffWindow(
            MatchSchedule match,
            List<MarathonbetPrematchEvent> events
    ) {
        if (match.getUtcKickoff() == null || events == null) {
            return List.of();
        }
        long center = match.getUtcKickoff().toEpochMilli();
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
            MatchSchedule match,
            List<MarathonbetPrematchEvent> matched
    ) {
        if (match.getUtcKickoff() == null) {
            return Optional.empty();
        }
        long center = match.getUtcKickoff().toEpochMilli();
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

    private boolean sidesMatch(MatchSchedule match, MarathonbetPrematchEvent event) {
        return sideMatches(match, event, true) && sideMatches(match, event, false);
    }

    /**
     * Only truth source: scraped Marathonbet name ↔ {@code external_aliases} provider {@code marathonbet}.
     */
    private boolean sideMatches(MatchSchedule match, MarathonbetPrematchEvent event, boolean home) {
        String marathonName = home ? event.getHomeTeam() : event.getAwayTeam();
        String teamId = home ? match.getHomeTeamId() : match.getAwayTeamId();
        if (teamId == null || teamId.isBlank() || marathonName == null || marathonName.isBlank()) {
            return false;
        }
        Optional<Team> byAlias = teamAliasResolver.resolveMarathonbetByName(marathonName);
        return byAlias.isPresent() && teamId.equals(byAlias.get().getId());
    }
}
