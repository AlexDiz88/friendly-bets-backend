package net.friendly_bets.oddsapi;

import lombok.RequiredArgsConstructor;
import net.friendly_bets.gameresults.ApiSyncIssueService;
import net.friendly_bets.models.Team;
import net.friendly_bets.models.TeamDisplayNames;
import net.friendly_bets.models.gameresults.GameResultRecord;
import net.friendly_bets.models.gameresults.GameResultSideSnapshot;
import net.friendly_bets.models.gameresults.GameResultSourceSnapshot;
import net.friendly_bets.oddsapi.client.dto.OddsApiEventDto;
import net.friendly_bets.oddsapi.config.OddsApiProperties;
import net.friendly_bets.repositories.GameResultRecordRepository;
import net.friendly_bets.services.GetEntityService;
import net.friendly_bets.services.TeamAliasResolver;
import net.friendly_bets.utils.TeamTitleUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class OddsApiEventMatcher {

    private static final Logger log = LoggerFactory.getLogger(OddsApiEventMatcher.class);

    private final TeamAliasResolver teamAliasResolver;
    private final GetEntityService getEntityService;
    private final ApiSyncIssueService apiSyncIssueService;
    private final GameResultRecordRepository gameResultRecordRepository;
    private final OddsApiProperties properties;

    public Optional<Long> resolveAndPersistEventId(
            GameResultRecord match,
            List<OddsApiEventDto> leagueEvents,
            String leagueCode,
            String season,
            int matchday
    ) {
        return resolveAndPersistEventId(match, leagueEvents, leagueCode, season, matchday, null);
    }

    public Optional<Long> resolveAndPersistEventId(
            GameResultRecord match,
            List<OddsApiEventDto> leagueEvents,
            String leagueCode,
            String season,
            int matchday,
            OddsTeamMappingCollector teamMappingCollector
    ) {
        if (match == null) {
            return Optional.empty();
        }
        if (match.getOddsApiEventId() != null && match.getOddsApiEventId() > 0) {
            return Optional.of(match.getOddsApiEventId());
        }

        List<OddsApiEventDto> candidates = filterByKickoffWindow(match, leagueEvents);
        List<OddsApiEventDto> matched = new ArrayList<>();
        for (OddsApiEventDto event : candidates) {
            if (event == null || event.getId() == null) {
                continue;
            }
            if (sidesMatch(match, event, teamMappingCollector)) {
                matched.add(event);
            }
        }

        if (matched.size() == 1) {
            Long eventId = matched.get(0).getId();
            match.setOddsApiEventId(eventId);
            gameResultRecordRepository.save(match);
            return Optional.of(eventId);
        }

        if (matched.isEmpty()) {
            apiSyncIssueService.recordOddsEventMappingMissing(match, leagueCode, season, matchday);
        } else {
            apiSyncIssueService.recordOddsEventMappingMissing(
                    match,
                    leagueCode,
                    season,
                    matchday,
                    "ambiguousOddsApiEventMatch"
            );
        }
        return Optional.empty();
    }

    /**
     * События odds-api, попадающие в окно kick-off хотя бы одного матча тура (не весь турнир).
     */
    public List<OddsApiEventDto> eventsForPendingMatches(
            List<GameResultRecord> pending,
            List<OddsApiEventDto> leagueEvents
    ) {
        if (pending == null || pending.isEmpty() || leagueEvents == null || leagueEvents.isEmpty()) {
            return List.of();
        }
        LinkedHashSet<Long> seenIds = new LinkedHashSet<>();
        List<OddsApiEventDto> result = new ArrayList<>();
        for (GameResultRecord match : pending) {
            for (OddsApiEventDto event : filterByKickoffWindow(match, leagueEvents)) {
                if (event != null && event.getId() != null && seenIds.add(event.getId())) {
                    result.add(event);
                }
            }
        }
        return result;
    }

    private List<OddsApiEventDto> filterByKickoffWindow(GameResultRecord match, List<OddsApiEventDto> events) {
        if (events == null) {
            return List.of();
        }
        if (match.getUtcDate() == null) {
            return List.of();
        }
        long center = match.getUtcDate().toInstant(ZoneOffset.UTC).toEpochMilli();
        long windowMs = properties.getEventWindowHours() * 3_600_000L;
        List<OddsApiEventDto> filtered = new ArrayList<>();
        for (OddsApiEventDto event : events) {
            Long kickoff = parseEventMillis(event.getDate());
            if (kickoff != null && Math.abs(kickoff - center) <= windowMs) {
                filtered.add(event);
            }
        }
        return filtered;
    }

    private boolean sidesMatch(
            GameResultRecord match,
            OddsApiEventDto event,
            OddsTeamMappingCollector teamMappingCollector
    ) {
        return sideMatches(match, event, true, teamMappingCollector)
                && sideMatches(match, event, false, teamMappingCollector);
    }

    private boolean sideMatches(
            GameResultRecord match,
            OddsApiEventDto event,
            boolean home,
            OddsTeamMappingCollector teamMappingCollector
    ) {
        String apiName = home ? event.getHome() : event.getAway();
        Integer apiId = home ? event.getHomeId() : event.getAwayId();
        String teamId = home ? match.getHomeTeamId() : match.getAwayTeamId();
        String fdName = externalName(match, home);

        recordMissingOddsApiAliases(match, home, apiName, apiId, teamId, teamMappingCollector);

        if (apiId != null && apiId > 0) {
            Optional<Team> byId = teamAliasResolver.resolveOddsApiById(apiId);
            if (byId.isPresent() && teamId != null && teamId.equals(byId.get().getId())) {
                return true;
            }
        }

        Optional<Team> byName = teamAliasResolver.resolveOddsApiByName(apiName);
        if (byName.isPresent() && teamId != null && teamId.equals(byName.get().getId())) {
            return true;
        }

        if (teamId != null && !teamId.isBlank()) {
            try {
                Team team = getEntityService.getTeamOrThrow(teamId);
                if (matchesInternalTeam(team, apiName, apiId)) {
                    return true;
                }
            } catch (Exception ignored) {
                // fall through to name-based match
            }
        }

        return TeamNameNormalizer.namesMatch(fdName, apiName);
    }

    private void recordMissingOddsApiAliases(
            GameResultRecord match,
            boolean home,
            String apiName,
            Integer apiId,
            String internalTeamId,
            OddsTeamMappingCollector teamMappingCollector
    ) {
        if (apiName == null || apiName.isBlank()) {
            return;
        }
        if (teamAliasResolver.oddsApiAliasesMapped(apiId, apiName)) {
            return;
        }

        String side = home ? "home" : "away";
        boolean idMissing = apiId != null && apiId > 0 && teamAliasResolver.resolveOddsApiById(apiId).isEmpty();
        boolean nameMissing = teamAliasResolver.resolveOddsApiByName(apiName).isEmpty();

        String sideKey = match.getId() + ":" + side + ":" + apiId + ":" + apiName;
        if (teamMappingCollector != null && !teamMappingCollector.registerSideIssue(sideKey)) {
            return;
        }

        int issueNumber = teamMappingCollector != null ? teamMappingCollector.getIssueCount() : 0;
        OddsTeamMappingLog.logIssue(
                log,
                issueNumber,
                match,
                side,
                internalTeamId,
                apiName,
                apiId,
                idMissing,
                nameMissing
        );

        apiSyncIssueService.recordOddsTeamMappingMissing(match, home, apiName, apiId);
    }

    private boolean matchesInternalTeam(Team team, String apiName, Integer apiId) {
        if (team == null) {
            return false;
        }
        if (apiId != null && apiId > 0) {
            Optional<Team> byId = teamAliasResolver.resolveOddsApiById(apiId);
            if (byId.isPresent()) {
                return team.getId().equals(byId.get().getId());
            }
        }
        if (apiName != null && TeamNameNormalizer.namesMatch(TeamTitleUtils.effectiveTitle(team), apiName)) {
            return true;
        }
        TeamDisplayNames display = team.getDisplayNames();
        if (display != null) {
            if (TeamNameNormalizer.namesMatch(display.getRu(), apiName)
                    || TeamNameNormalizer.namesMatch(display.getEn(), apiName)
                    || TeamNameNormalizer.namesMatch(display.getDe(), apiName)) {
                return true;
            }
        }
        return teamAliasResolver.resolveOddsApiByName(apiName)
                .map(t -> t.getId().equals(team.getId()))
                .orElse(false);
    }

    private static String externalName(GameResultRecord match, boolean home) {
        GameResultSourceSnapshot source = match.primaryExternalSource();
        if (source == null) {
            return null;
        }
        GameResultSideSnapshot side = home ? source.getHome() : source.getAway();
        return side != null ? side.getExternalName() : null;
    }

    private static Long parseEventMillis(String date) {
        if (date == null || date.isBlank()) {
            return null;
        }
        try {
            return Instant.parse(date).toEpochMilli();
        } catch (DateTimeParseException e) {
            try {
                return LocalDateTime.parse(date).toInstant(ZoneOffset.UTC).toEpochMilli();
            } catch (DateTimeParseException ignored) {
                return null;
            }
        }
    }
}
