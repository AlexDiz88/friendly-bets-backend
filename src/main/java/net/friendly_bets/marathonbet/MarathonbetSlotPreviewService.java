package net.friendly_bets.marathonbet;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.RequiredArgsConstructor;
import net.friendly_bets.dto.MarathonbetSlotMatchPreviewDto;
import net.friendly_bets.dto.MarathonbetSlotPreviewDto;
import net.friendly_bets.exceptions.BadRequestException;
import net.friendly_bets.gameresults.MatchdaySlotSupport;
import net.friendly_bets.marathonbet.client.MarathonbetTournamentClient;
import net.friendly_bets.marathonbet.config.MarathonbetProperties;
import net.friendly_bets.models.League;
import net.friendly_bets.models.Season;
import net.friendly_bets.models.odds.Odds;
import net.friendly_bets.models.schedule.MatchSchedule;
import net.friendly_bets.repositories.OddsRepository;
import net.friendly_bets.services.GetEntityService;
import net.friendly_bets.services.MatchScheduleQueryService;
import net.friendly_bets.services.RunningSeasonLookup;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class MarathonbetSlotPreviewService {

    private final MarathonbetProperties properties;
    private final MarathonbetTournamentClient tournamentClient;
    private final MarathonbetEventMatcher eventMatcher;
    private final MatchScheduleQueryService matchScheduleQueryService;
    private final OddsRepository oddsRepository;
    private final GetEntityService getEntityService;
    private final RunningSeasonLookup runningSeasonLookup;
    private final MatchdaySlotSupport matchdaySupport;

    public MarathonbetSlotPreviewDto buildPreview(String leagueId, int matchday, String season) {
        League league = getEntityService.getLeagueOrThrow(leagueId);
        if (league.getLeagueCode() == null) {
            throw new BadRequestException("marathonbetInvalidTournamentId");
        }
        String code = league.getLeagueCode().name();
        Long tournamentId = properties.getTournamentTreeIds().get(code);
        if (tournamentId == null || tournamentId <= 0) {
            throw new BadRequestException("marathonbetInvalidTournamentId");
        }
        String resolvedSeason = resolveSeason(season, league);
        JsonNode tournamentRoot = tournamentClient.fetchTournament(tournamentId).requireBody();
        List<MarathonbetPrematchEvent> prematch = MarathonbetTournamentParser.parsePrematchEvents(tournamentRoot);

        List<MatchSchedule> matches = matchScheduleQueryService.getMatches(
                code,
                matchday,
                resolvedSeason,
                league.getId()
        );

        List<MarathonbetSlotMatchPreviewDto> rows = new ArrayList<>();
        for (MatchSchedule match : matches) {
            Optional<MarathonbetPrematchEvent> mapped = eventMatcher.resolve(match, prematch);
            String homeTitle = match.getHomeTeamId() != null
                    ? getEntityService.getTeamOrThrow(match.getHomeTeamId()).getTitle()
                    : null;
            String awayTitle = match.getAwayTeamId() != null
                    ? getEntityService.getTeamOrThrow(match.getAwayTeamId()).getTitle()
                    : null;

            MarathonbetPrematchEvent event = mapped.orElse(null);
            Long cachedTreeId = oddsRepository.findByMatchScheduleId(match.getId())
                    .map(Odds::getMarathonbetTreeId)
                    .orElse(null);
            LocalDateTime utcDate = match.getUtcKickoff() != null
                    ? LocalDateTime.ofInstant(match.getUtcKickoff(), ZoneOffset.UTC)
                    : null;
            rows.add(MarathonbetSlotMatchPreviewDto.builder()
                    .matchScheduleId(match.getId())
                    .matchday(match.getMatchday())
                    .homeTeamTitle(homeTitle)
                    .awayTeamTitle(awayTitle)
                    .utcDate(utcDate)
                    .marathonbetTreeId(event != null ? event.getTreeId() : cachedTreeId)
                    .marathonHomeTeam(event != null ? event.getHomeTeam() : null)
                    .marathonAwayTeam(event != null ? event.getAwayTeam() : null)
                    .marathonDisplayTimeMillis(event != null ? event.getDisplayTimeMillis() : null)
                    .matchStatus(match.getStatus())
                    .mappingOk(mapped.isPresent())
                    .mappingNote(mapped.isPresent() ? null : "eventMappingMissing")
                    .build());
        }

        return MarathonbetSlotPreviewDto.builder()
                .leagueId(league.getId())
                .leagueCode(code)
                .season(resolvedSeason)
                .matchday(matchday)
                .tournamentTreeId(tournamentId)
                .matches(rows)
                .build();
    }

    private String resolveSeason(String requestedSeason, League league) {
        if (requestedSeason != null && !requestedSeason.isBlank()) {
            return requestedSeason.trim();
        }
        Season active = runningSeasonLookup.findRunningSeasonOrThrow("seasonDatesRequired");
        return matchdaySupport.resolveExternalSeasonYear(active, league.getLeagueCode());
    }
}
