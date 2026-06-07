package net.friendly_bets.marathonbet;

import lombok.RequiredArgsConstructor;
import net.friendly_bets.footballdata.ApiSyncIssueService;
import net.friendly_bets.marathonbet.config.MarathonbetProperties;
import net.friendly_bets.models.Team;
import net.friendly_bets.models.TeamDisplayNames;
import net.friendly_bets.models.gameresults.GameResultRecord;
import net.friendly_bets.models.gameresults.GameResultSideSnapshot;
import net.friendly_bets.models.gameresults.GameResultSourceSnapshot;
import net.friendly_bets.oddsapi.TeamNameNormalizer;
import net.friendly_bets.repositories.GameResultRecordRepository;
import net.friendly_bets.services.GetEntityService;
import net.friendly_bets.services.TeamAliasResolver;
import net.friendly_bets.wc26.Wc26TeamCatalog;
import org.springframework.stereotype.Component;

import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class MarathonbetEventMatcher {

    private final TeamAliasResolver teamAliasResolver;
    private final GetEntityService getEntityService;
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
        long center = match.getUtcDate().toInstant(ZoneOffset.UTC).toEpochMilli();
        long windowMs = properties.getEventWindowHours() * 3_600_000L;
        List<MarathonbetPrematchEvent> filtered = new ArrayList<>();
        for (MarathonbetPrematchEvent event : events) {
            Long kickoff = event.getDisplayTimeMillis();
            if (kickoff != null && Math.abs(kickoff - center) <= windowMs) {
                filtered.add(event);
            }
        }
        return filtered;
    }

    private boolean sidesMatch(GameResultRecord match, MarathonbetPrematchEvent event) {
        return sideMatches(match, event, true) && sideMatches(match, event, false);
    }

    private boolean sideMatches(GameResultRecord match, MarathonbetPrematchEvent event, boolean home) {
        String marathonName = home ? event.getHomeTeam() : event.getAwayTeam();
        String teamId = home ? match.getHomeTeamId() : match.getAwayTeamId();
        String fdName = externalName(match, home);

        Optional<Team> byAlias = teamAliasResolver.resolveMarathonbetByName(marathonName);
        if (byAlias.isPresent() && teamId != null && teamId.equals(byAlias.get().getId())) {
            return true;
        }

        Optional<String> fifaCode = MarathonbetTeamCatalog.fifaCodeForMarathonName(marathonName);
        if (fifaCode.isPresent() && teamId != null && !teamId.isBlank()) {
            Optional<Team> byFifa = teamAliasResolver.resolveWc26Code(fifaCode.get());
            if (byFifa.isPresent() && teamId.equals(byFifa.get().getId())) {
                return true;
            }
            for (String oddsName : Wc26TeamCatalog.oddsApiNameCandidatesForFifaCode(fifaCode.get())) {
                Optional<Team> byOdds = teamAliasResolver.resolveOddsApiByName(oddsName);
                if (byOdds.isPresent() && teamId.equals(byOdds.get().getId())) {
                    return true;
                }
            }
        }

        if (teamId != null && !teamId.isBlank()) {
            try {
                Team team = getEntityService.getTeamOrThrow(teamId);
                if (matchesInternalTeam(team, marathonName)) {
                    return true;
                }
            } catch (Exception ignored) {
                // fall through
            }
        }

        return TeamNameNormalizer.namesMatch(fdName, marathonName);
    }

    private boolean matchesInternalTeam(Team team, String marathonName) {
        if (team == null || marathonName == null) {
            return false;
        }
        TeamDisplayNames names = team.getDisplayNames();
        if (names != null) {
            if (TeamNameNormalizer.namesMatch(names.getRu(), marathonName)) {
                return true;
            }
            if (TeamNameNormalizer.namesMatch(names.getEn(), marathonName)) {
                return true;
            }
        }
        return TeamNameNormalizer.namesMatch(team.getTitle(), marathonName);
    }

    private static String externalName(GameResultRecord match, boolean home) {
        GameResultSourceSnapshot source = match.footballDataSource();
        if (source == null) {
            return null;
        }
        GameResultSideSnapshot side = home ? source.getHome() : source.getAway();
        return side != null ? side.getExternalName() : null;
    }
}
