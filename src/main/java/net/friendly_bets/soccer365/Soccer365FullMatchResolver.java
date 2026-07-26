package net.friendly_bets.soccer365;

import lombok.RequiredArgsConstructor;
import net.friendly_bets.exceptions.BadRequestException;
import net.friendly_bets.gameresults.MatchDataProviders;
import net.friendly_bets.models.League;
import net.friendly_bets.models.Team;
import net.friendly_bets.models.schedule.MatchSchedule;
import net.friendly_bets.providers.ProviderMatchResolveSupport;
import net.friendly_bets.services.GetEntityService;
import net.friendly_bets.services.TeamAliasResolver;
import net.friendly_bets.soccer365.config.Soccer365Properties;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

/**
 * Resolves soccer365 game page id from competition schedule via kickoff + soccer365 team aliases.
 * Does not read or write {@code match_schedules.external_ids}.
 */
@Component
@RequiredArgsConstructor
public class Soccer365FullMatchResolver {

    private final Soccer365Properties properties;
    private final Soccer365HttpClient httpClient;
    private final Soccer365ScheduleParser scheduleParser;
    private final Soccer365TeamNamesService teamNamesService;
    private final TeamAliasResolver teamAliasResolver;
    private final GetEntityService getEntityService;

    public String resolveGameId(MatchSchedule match) {
        if (match == null) {
            throw new BadRequestException("matchScheduleNotFound");
        }
        if (match.getUtcKickoff() == null) {
            throw new BadRequestException("fullMatchKickoffRequired");
        }
        League.LeagueCode leagueCode = parseLeagueCode(match.getLeagueCode());
        int competitionId = teamNamesService.requireCompetitionId(leagueCode);
        String html = httpClient.fetchScheduleHtml(competitionId);
        Soccer365ParsedSchedule parsed = scheduleParser.parse(html, competitionId);

        List<Soccer365ParsedSchedule.Match> flat = new ArrayList<>();
        for (Soccer365ParsedSchedule.Round round : parsed.getRounds()) {
            if (round.getMatches() != null) {
                flat.addAll(round.getMatches());
            }
        }

        Team home = getEntityService.getTeamOrThrow(match.getHomeTeamId());
        Team away = getEntityService.getTeamOrThrow(match.getAwayTeamId());
        Duration window = Duration.ofMinutes(Math.max(1, properties.getFullMatchKickoffWindowMinutes()));

        ProviderMatchResolveSupport.ResolveOutcome<Soccer365ParsedSchedule.Match> outcome =
                ProviderMatchResolveSupport.resolveUnique(
                        match,
                        flat,
                        window,
                        Soccer365ParsedSchedule.Match::getUtcKickoff,
                        candidate -> sidesMatch(home, away, candidate)
                );

        if (outcome.isUnique() && outcome.match() != null) {
            return requireGameId(outcome.match());
        }
        if (outcome.isAmbiguous()) {
            throw new BadRequestException("fullMatchAmbiguous");
        }

        // soccer365 often omits JSON-LD kickoff past the first page chunk; fall back to
        // unique home/away alias match within the same matchday round only.
        Soccer365ParsedSchedule.Round round = parsed.roundsByNumber().get(match.getMatchday());
        if (round == null || round.getMatches() == null || round.getMatches().isEmpty()) {
            throw new BadRequestException("fullMatchNotFound");
        }
        List<Soccer365ParsedSchedule.Match> bySides = new ArrayList<>();
        for (Soccer365ParsedSchedule.Match candidate : round.getMatches()) {
            if (sidesMatch(home, away, candidate)) {
                bySides.add(candidate);
            }
        }
        if (bySides.isEmpty()) {
            throw new BadRequestException("fullMatchNotFound");
        }
        if (bySides.size() > 1) {
            throw new BadRequestException("fullMatchAmbiguous");
        }
        return requireGameId(bySides.get(0));
    }

    private static String requireGameId(Soccer365ParsedSchedule.Match match) {
        String gameId = match.getSoccer365GameId();
        if (gameId == null || gameId.isBlank()) {
            throw new BadRequestException("fullMatchNotFound");
        }
        return gameId.trim();
    }

    private boolean sidesMatch(Team home, Team away, Soccer365ParsedSchedule.Match candidate) {
        return teamAliasResolver.teamMatchesScoreProviderSide(
                home, MatchDataProviders.SOCCER365, candidate.getHomeName())
                && teamAliasResolver.teamMatchesScoreProviderSide(
                away, MatchDataProviders.SOCCER365, candidate.getAwayName());
    }

    private static League.LeagueCode parseLeagueCode(String raw) {
        if (raw == null || raw.isBlank()) {
            throw new BadRequestException("leagueCodeRequired");
        }
        try {
            return League.LeagueCode.valueOf(raw.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new BadRequestException("invalidLeagueCode");
        }
    }
}
