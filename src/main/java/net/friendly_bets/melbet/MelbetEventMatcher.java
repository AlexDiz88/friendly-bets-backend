package net.friendly_bets.melbet;

import lombok.RequiredArgsConstructor;
import net.friendly_bets.melbet.config.MelbetProperties;
import net.friendly_bets.models.Team;
import net.friendly_bets.models.odds.Odds;
import net.friendly_bets.models.schedule.MatchSchedule;
import net.friendly_bets.providers.ExternalProviderIds;
import net.friendly_bets.providers.ProviderMatchResolveSupport;
import net.friendly_bets.repositories.OddsRepository;
import net.friendly_bets.services.ErrorLogService;
import net.friendly_bets.services.TeamAliasResolver;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
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

        Duration window = Duration.ofHours(Math.max(0, properties.getEventWindowHours()));
        boolean anyInWindow = ProviderMatchResolveSupport.anyKickoffInWindow(
                match, tournamentEvents, window, MelbetEventMatcher::eventKickoff);
        List<MelbetPrematchEvent> bySides = sidesMatches(match, tournamentEvents);
        if (bySides.isEmpty() && !anyInWindow) {
            return MelbetEventResolveResult.miss(MelbetEventResolveResult.MissKind.NO_BOOKIE_EVENT);
        }
        if (bySides.size() > 1) {
            errorLogService.recordEventMappingMissing(
                    match, ExternalProviderIds.MELBET, leagueCode, season, matchday, "ambiguousMelbetEventMatch");
        } else {
            errorLogService.recordEventMappingMissing(
                    match, ExternalProviderIds.MELBET, leagueCode, season, matchday, null);
        }
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
        if (cachedEventId != null && cachedEventId > 0 && tournamentEvents != null) {
            Optional<MelbetPrematchEvent> byId = tournamentEvents.stream()
                    .filter(e -> e.getEventId() == cachedEventId)
                    .findFirst();
            if (byId.isPresent()) {
                return byId;
            }
        }

        Duration window = Duration.ofHours(Math.max(0, properties.getEventWindowHours()));
        ProviderMatchResolveSupport.ResolveOutcome<MelbetPrematchEvent> outcome =
                ProviderMatchResolveSupport.resolveUniquePreferringKickoffWindow(
                        match,
                        tournamentEvents != null ? tournamentEvents : List.of(),
                        window,
                        MelbetEventMatcher::eventKickoff,
                        event -> sidesMatch(match, event)
                );
        if (outcome.isUnique()) {
            return Optional.of(outcome.match());
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

    private List<MelbetPrematchEvent> sidesMatches(MatchSchedule match, List<MelbetPrematchEvent> events) {
        if (events == null || events.isEmpty()) {
            return List.of();
        }
        List<MelbetPrematchEvent> matched = new ArrayList<>();
        for (MelbetPrematchEvent event : events) {
            if (sidesMatch(match, event)) {
                matched.add(event);
            }
        }
        return matched;
    }

    private static Instant eventKickoff(MelbetPrematchEvent event) {
        return event != null ? event.getKickoff() : null;
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
        return matchesAlias(teamId, primary) || matchesAlias(teamId, english);
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
