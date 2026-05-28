package net.friendly_bets.oddsapi;

import lombok.RequiredArgsConstructor;
import net.friendly_bets.footballdata.ApiSyncIssueService;
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
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class OddsApiEventMatcher {

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
            if (sidesMatch(match, event)) {
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

    private List<OddsApiEventDto> filterByKickoffWindow(GameResultRecord match, List<OddsApiEventDto> events) {
        if (match.getUtcDate() == null || events == null) {
            return events != null ? events : List.of();
        }
        long center = match.getUtcDate().toInstant(ZoneOffset.UTC).toEpochMilli();
        long windowMs = properties.getEventWindowHours() * 3_600_000L;
        List<OddsApiEventDto> filtered = new ArrayList<>();
        for (OddsApiEventDto event : events) {
            Long kickoff = parseEventMillis(event.getDate());
            if (kickoff == null || Math.abs(kickoff - center) <= windowMs) {
                filtered.add(event);
            }
        }
        return filtered.isEmpty() ? events : filtered;
    }

    private boolean sidesMatch(GameResultRecord match, OddsApiEventDto event) {
        return sideMatches(match, event, true) && sideMatches(match, event, false);
    }

    private boolean sideMatches(GameResultRecord match, OddsApiEventDto event, boolean home) {
        String apiName = home ? event.getHome() : event.getAway();
        Integer apiId = home ? event.getHomeId() : event.getAwayId();
        String teamId = home ? match.getHomeTeamId() : match.getAwayTeamId();
        String fdName = externalName(match, home);

        Optional<Team> byAlias = teamAliasResolver.resolveOddsApi(apiId, apiName);
        if (byAlias.isPresent() && teamId != null) {
            return teamId.equals(byAlias.get().getId());
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

        if (TeamNameNormalizer.namesMatch(fdName, apiName)) {
            return true;
        }

        if (apiName != null && !apiName.isBlank()) {
            apiSyncIssueService.recordOddsTeamMappingMissing(
                    match,
                    home,
                    apiName,
                    apiId
            );
        }
        return false;
    }

    private boolean matchesInternalTeam(Team team, String apiName, Integer apiId) {
        if (team == null) {
            return false;
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
        return teamAliasResolver.resolveOddsApi(apiId, apiName)
                .map(t -> t.getId().equals(team.getId()))
                .orElse(false);
    }

    private static String externalName(GameResultRecord match, boolean home) {
        GameResultSourceSnapshot source = match.footballDataSource();
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
