package net.friendly_bets.melbet;

import lombok.RequiredArgsConstructor;
import net.friendly_bets.melbet.config.MelbetProperties;
import net.friendly_bets.models.Team;
import net.friendly_bets.models.odds.Odds;
import net.friendly_bets.models.schedule.MatchSchedule;
import net.friendly_bets.providers.ExternalProviderIds;
import net.friendly_bets.repositories.OddsRepository;
import net.friendly_bets.services.ErrorLogService;
import net.friendly_bets.services.TeamAliasResolver;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class MelbetEventMatcher {

    private final TeamAliasResolver teamAliasResolver;
    private final ErrorLogService errorLogService;
    private final OddsRepository oddsRepository;
    private final MelbetProperties properties;

    public MelbetEventResolveResult resolveAndRecordMappingIssue(
            MatchSchedule match,
            List<MelbetPrematchEvent> tournamentEvents,
            String leagueCode,
            String season,
            int matchday
    ) {
        Optional<MelbetPrematchEvent> resolved = resolve(match, tournamentEvents);
        if (resolved.isPresent()) {
            return MelbetEventResolveResult.matched(resolved.get());
        }
        if (match == null || match.getId() == null) {
            return MelbetEventResolveResult.miss(MelbetEventResolveResult.MissKind.MAPPING_FAILURE);
        }
        if (match.getUtcKickoff() == null) {
            return MelbetEventResolveResult.miss(MelbetEventResolveResult.MissKind.NO_BOOKIE_EVENT);
        }

        List<MelbetPrematchEvent> candidates = filterByKickoffWindow(match, tournamentEvents);
        if (candidates.isEmpty()) {
            return MelbetEventResolveResult.miss(MelbetEventResolveResult.MissKind.NO_BOOKIE_EVENT);
        }

        List<MelbetPrematchEvent> matched = new ArrayList<>();
        for (MelbetPrematchEvent event : candidates) {
            if (sidesMatch(match, event)) {
                matched.add(event);
            }
        }
        if (matched.size() > 1) {
            Optional<MelbetPrematchEvent> disambiguated = pickClosestKickoff(match, matched);
            if (disambiguated.isPresent()) {
                return MelbetEventResolveResult.matched(disambiguated.get());
            }
            errorLogService.recordEventMappingMissing(
                    match, ExternalProviderIds.MELBET, leagueCode, season, matchday, "ambiguousMelbetEventMatch");
            return MelbetEventResolveResult.miss(MelbetEventResolveResult.MissKind.MAPPING_FAILURE);
        }

        errorLogService.recordEventMappingMissing(
                match, ExternalProviderIds.MELBET, leagueCode, season, matchday, null);
        return MelbetEventResolveResult.miss(MelbetEventResolveResult.MissKind.MAPPING_FAILURE);
    }

    public Optional<MelbetPrematchEvent> resolve(
            MatchSchedule match,
            List<MelbetPrematchEvent> tournamentEvents
    ) {
        if (match == null) {
            return Optional.empty();
        }
        Long cachedEventId = resolveCachedEventId(match.getId());
        if (cachedEventId != null && cachedEventId > 0) {
            Optional<MelbetPrematchEvent> byId = tournamentEvents.stream()
                    .filter(e -> e.getEventId() == cachedEventId)
                    .findFirst();
            if (byId.isPresent()) {
                return byId;
            }
        }

        List<MelbetPrematchEvent> candidates = filterByKickoffWindow(match, tournamentEvents);
        List<MelbetPrematchEvent> matched = new ArrayList<>();
        for (MelbetPrematchEvent event : candidates) {
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

    private Long resolveCachedEventId(String matchScheduleId) {
        if (matchScheduleId == null || matchScheduleId.isBlank()) {
            return null;
        }
        return oddsRepository.findByMatchScheduleId(matchScheduleId)
                .map(Odds::getMelbetEventId)
                .orElse(null);
    }

    private List<MelbetPrematchEvent> filterByKickoffWindow(
            MatchSchedule match,
            List<MelbetPrematchEvent> events
    ) {
        if (match.getUtcKickoff() == null || events == null) {
            return List.of();
        }
        long center = match.getUtcKickoff().toEpochMilli();
        long windowMs = properties.getEventWindowHours() * 3_600_000L;
        List<MelbetPrematchEvent> filtered = new ArrayList<>();
        for (MelbetPrematchEvent event : events) {
            Long eventKickoff = event.kickoffEpochMillis();
            if (eventKickoff != null && Math.abs(eventKickoff - center) <= windowMs) {
                filtered.add(event);
            }
        }
        return filtered;
    }

    private Optional<MelbetPrematchEvent> pickClosestKickoff(
            MatchSchedule match,
            List<MelbetPrematchEvent> matched
    ) {
        if (match.getUtcKickoff() == null) {
            return Optional.empty();
        }
        long center = match.getUtcKickoff().toEpochMilli();
        MelbetPrematchEvent best = null;
        long bestDelta = Long.MAX_VALUE;
        long secondBestDelta = Long.MAX_VALUE;
        for (MelbetPrematchEvent event : matched) {
            Long eventKickoff = event.kickoffEpochMillis();
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
        if (secondBestDelta == Long.MAX_VALUE || bestDelta + 3_600_000 <= secondBestDelta) {
            return Optional.of(best);
        }
        return Optional.empty();
    }

    private boolean sidesMatch(MatchSchedule match, MelbetPrematchEvent event) {
        return sideMatches(match, event, true) && sideMatches(match, event, false);
    }

    private boolean sideMatches(MatchSchedule match, MelbetPrematchEvent event, boolean home) {
        String teamId = home ? match.getHomeTeamId() : match.getAwayTeamId();
        if (teamId == null || teamId.isBlank()) {
            return false;
        }
        String primary = home ? event.getHomeTeam() : event.getAwayTeam();
        String english = home ? event.getHomeTeamEn() : event.getAwayTeamEn();
        if (matchesAlias(teamId, primary) || matchesAlias(teamId, english)) {
            return true;
        }
        return false;
    }

    private boolean matchesAlias(String teamId, String providerName) {
        if (providerName == null || providerName.isBlank()) {
            return false;
        }
        Optional<Team> byAlias = teamAliasResolver.resolveByProviderName(
                ExternalProviderIds.MELBET, providerName);
        return byAlias.isPresent() && teamId.equals(byAlias.get().getId());
    }
}
