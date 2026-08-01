package net.friendly_bets.marathonbet;

import lombok.RequiredArgsConstructor;
import net.friendly_bets.marathonbet.config.MarathonbetProperties;
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
            return MarathonbetEventResolveResult.miss(MarathonbetEventResolveResult.MissKind.NO_BOOKIE_EVENT);
        }

        Duration window = Duration.ofHours(Math.max(0, properties.getEventWindowHours()));
        boolean anyInWindow = ProviderMatchResolveSupport.anyKickoffInWindow(
                match, tournamentEvents, window, MarathonbetEventMatcher::eventKickoff);
        List<MarathonbetPrematchEvent> bySides = sidesMatches(match, tournamentEvents);
        if (bySides.isEmpty() && !anyInWindow) {
            return MarathonbetEventResolveResult.miss(MarathonbetEventResolveResult.MissKind.NO_BOOKIE_EVENT);
        }
        if (bySides.size() > 1) {
            errorLogService.recordEventMappingMissing(
                    match, "marathonbet", leagueCode, season, matchday, "ambiguousMarathonbetEventMatch");
        } else {
            errorLogService.recordEventMappingMissing(
                    match, "marathonbet", leagueCode, season, matchday, null);
        }
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
        if (cachedTreeId != null && cachedTreeId > 0 && tournamentEvents != null) {
            Optional<MarathonbetPrematchEvent> byTreeId = tournamentEvents.stream()
                    .filter(e -> e.getTreeId() == cachedTreeId)
                    .findFirst();
            if (byTreeId.isPresent()) {
                return byTreeId;
            }
        }

        Duration window = Duration.ofHours(Math.max(0, properties.getEventWindowHours()));
        ProviderMatchResolveSupport.ResolveOutcome<MarathonbetPrematchEvent> outcome =
                ProviderMatchResolveSupport.resolveUniquePreferringKickoffWindow(
                        match,
                        tournamentEvents != null ? tournamentEvents : List.of(),
                        window,
                        MarathonbetEventMatcher::eventKickoff,
                        event -> sidesMatch(match, event)
                );
        if (outcome.isUnique()) {
            return Optional.of(outcome.match());
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
        Duration window = Duration.ofHours(Math.max(0, properties.getEventWindowHours()));
        LinkedHashSet<Long> seen = new LinkedHashSet<>();
        List<MarathonbetPrematchEvent> result = new ArrayList<>();
        for (MatchSchedule match : pending) {
            for (MarathonbetPrematchEvent event : tournamentEvents) {
                Instant eventKickoff = eventKickoff(event);
                if (eventKickoff == null || match.getUtcKickoff() == null) {
                    continue;
                }
                long delta = Math.abs(Duration.between(match.getUtcKickoff(), eventKickoff).getSeconds());
                if (delta <= window.getSeconds() && seen.add(event.getTreeId())) {
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

    private List<MarathonbetPrematchEvent> sidesMatches(
            MatchSchedule match,
            List<MarathonbetPrematchEvent> events
    ) {
        if (events == null || events.isEmpty()) {
            return List.of();
        }
        List<MarathonbetPrematchEvent> matched = new ArrayList<>();
        for (MarathonbetPrematchEvent event : events) {
            if (sidesMatch(match, event)) {
                matched.add(event);
            }
        }
        return matched;
    }

    private static Instant eventKickoff(MarathonbetPrematchEvent event) {
        if (event == null || event.getDisplayTimeMillis() == null) {
            return null;
        }
        return Instant.ofEpochMilli(event.getDisplayTimeMillis());
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
        Optional<Team> byAlias = teamAliasResolver.resolveByProviderName(ExternalProviderIds.MARATHONBET, marathonName);
        return byAlias.isPresent() && teamId.equals(byAlias.get().getId());
    }
}
