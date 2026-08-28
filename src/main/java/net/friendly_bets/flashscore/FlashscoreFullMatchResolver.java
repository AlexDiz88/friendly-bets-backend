package net.friendly_bets.flashscore;

import lombok.RequiredArgsConstructor;
import net.friendly_bets.exceptions.BadRequestException;
import net.friendly_bets.flashscore.config.FlashscoreProperties;
import net.friendly_bets.models.League;
import net.friendly_bets.models.Team;
import net.friendly_bets.models.schedule.MatchSchedule;
import net.friendly_bets.providers.ExternalProviderIds;
import net.friendly_bets.providers.ProviderMatchResolveSupport;
import net.friendly_bets.services.GetEntityService;
import net.friendly_bets.services.TeamAliasResolver;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

@Component
@RequiredArgsConstructor
public class FlashscoreFullMatchResolver {

    private final FlashscoreProperties properties;
    private final FlashscoreHttpClient httpClient;
    private final FlashscoreDayFeedParser dayFeedParser;
    private final TeamAliasResolver teamAliasResolver;
    private final GetEntityService getEntityService;

    public FlashscoreParsedDayPage.Match resolveMatch(MatchSchedule match) {
        if (match == null) {
            throw new BadRequestException("matchScheduleNotFound");
        }
        if (match.getUtcKickoff() == null) {
            throw new BadRequestException("missingUtcKickoff");
        }
        Instant kickoff = match.getUtcKickoff();
        int windowHours = Math.max(1, properties.getFullMatchKickoffWindowHours());
        Duration window = Duration.ofHours(windowHours);

        List<FlashscoreParsedDayPage.Match> candidates = new ArrayList<>();
        ZoneId feedZone = properties.feedZone();
        for (LocalDate day : daysCoveringWindow(kickoff, window, feedZone)) {
            String feed = httpClient.fetchDayFootballFeed(day);
            FlashscoreParsedDayPage page = dayFeedParser.parse(feed, day);
            if (page.getCompetitions() == null) {
                continue;
            }
            for (FlashscoreParsedDayPage.CompetitionBlock block : page.getCompetitions()) {
                if (block.getMatches() != null) {
                    candidates.addAll(block.getMatches());
                }
            }
        }

        Team home = getEntityService.getTeamOrThrow(match.getHomeTeamId());
        Team away = getEntityService.getTeamOrThrow(match.getAwayTeamId());

        ProviderMatchResolveSupport.ResolveOutcome<FlashscoreParsedDayPage.Match> outcome =
                ProviderMatchResolveSupport.resolveUniquePreferringKickoffWindow(
                        match,
                        candidates,
                        window,
                        FlashscoreParsedDayPage.Match::getUtcKickoff,
                        candidate -> sidesMatch(home, away, candidate)
                );

        if (outcome.isUnique() && outcome.match() != null) {
            return outcome.match();
        }
        if (outcome.isAmbiguous()) {
            throw new BadRequestException("fullMatchAmbiguous");
        }
        throw new BadRequestException("fullMatchNotFound");
    }

    static List<LocalDate> daysCoveringWindow(Instant kickoff, Duration window, ZoneId zone) {
        Instant from = kickoff.minus(window);
        Instant to = kickoff.plus(window);
        LocalDate start = from.atZone(zone).toLocalDate();
        LocalDate end = to.atZone(zone).toLocalDate();
        Set<LocalDate> days = new LinkedHashSet<>();
        for (LocalDate d = start; !d.isAfter(end); d = d.plusDays(1)) {
            days.add(d);
        }
        return new ArrayList<>(days);
    }

    private boolean sidesMatch(Team home, Team away, FlashscoreParsedDayPage.Match candidate) {
        return teamAliasResolver.teamMatchesProviderSide(
                home, ExternalProviderIds.FLASHSCORE, candidate.getHomeName())
                && teamAliasResolver.teamMatchesProviderSide(
                away, ExternalProviderIds.FLASHSCORE, candidate.getAwayName());
    }

    static League.LeagueCode parseLeagueCode(String raw) {
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
