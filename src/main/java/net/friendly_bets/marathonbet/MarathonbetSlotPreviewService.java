package net.friendly_bets.marathonbet;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.RequiredArgsConstructor;
import net.friendly_bets.dto.MarathonbetSlotMatchPreviewDto;
import net.friendly_bets.dto.MarathonbetSlotPreviewDto;
import net.friendly_bets.exceptions.BadRequestException;
import net.friendly_bets.gameresults.GameResultQueryService;
import net.friendly_bets.marathonbet.client.MarathonbetTournamentClient;
import net.friendly_bets.marathonbet.config.MarathonbetProperties;
import net.friendly_bets.models.League;
import net.friendly_bets.models.Season;
import net.friendly_bets.models.gameresults.GameResultRecord;
import net.friendly_bets.services.GetEntityService;
import net.friendly_bets.services.RunningSeasonLookup;
import net.friendly_bets.gameresults.MatchdaySlotSupport;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class MarathonbetSlotPreviewService {

    private final MarathonbetProperties properties;
    private final MarathonbetTournamentClient tournamentClient;
    private final MarathonbetEventMatcher eventMatcher;
    private final GameResultQueryService gameResultQueryService;
    private final GetEntityService getEntityService;
    private final RunningSeasonLookup runningSeasonLookup;
    private final MatchdaySlotSupport matchdaySupport;

    public MarathonbetSlotPreviewDto buildPreview(String leagueId, int matchday, String season) {
        League league = getEntityService.getLeagueOrThrow(leagueId);
        if (league.getLeagueCode() != League.LeagueCode.WC) {
            throw new BadRequestException("marathonbetWcOnly");
        }
        Long tournamentId = properties.getTournamentTreeIds().get("WC");
        if (tournamentId == null || tournamentId <= 0) {
            throw new BadRequestException("marathonbetInvalidTournamentId");
        }
        String resolvedSeason = resolveSeason(season, league);
        JsonNode tournamentRoot = tournamentClient.fetchTournament(tournamentId).requireBody();
        List<MarathonbetPrematchEvent> prematch = MarathonbetTournamentParser.parsePrematchEvents(tournamentRoot);

        List<GameResultRecord> matches = gameResultQueryService.getMatches(
                league.getLeagueCode().name(),
                matchday,
                resolvedSeason,
                league.getId()
        );

        List<MarathonbetSlotMatchPreviewDto> rows = new ArrayList<>();
        for (GameResultRecord match : matches) {
            Optional<MarathonbetPrematchEvent> mapped = eventMatcher.resolve(match, prematch);
            String homeTitle = match.getHomeTeamId() != null
                    ? getEntityService.getTeamOrThrow(match.getHomeTeamId()).getTitle()
                    : null;
            String awayTitle = match.getAwayTeamId() != null
                    ? getEntityService.getTeamOrThrow(match.getAwayTeamId()).getTitle()
                    : null;

            MarathonbetPrematchEvent event = mapped.orElse(null);
            rows.add(MarathonbetSlotMatchPreviewDto.builder()
                    .gameResultId(match.getId())
                    .matchday(match.getMatchday())
                    .homeTeamTitle(homeTitle)
                    .awayTeamTitle(awayTitle)
                    .utcDate(match.getUtcDate())
                    .marathonbetTreeId(match.getMarathonbetTreeId())
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
                .leagueCode(league.getLeagueCode().name())
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
