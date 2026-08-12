package net.friendly_bets.flashscore;

import lombok.RequiredArgsConstructor;
import net.friendly_bets.exceptions.BadRequestException;
import net.friendly_bets.flashscore.config.FlashscoreProperties;
import net.friendly_bets.models.League;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Team names for alias autobind — same canonical FH/FK labels as day feed / FULL_MATCH,
 * not localized labels from the tournament HTML page.
 */
@Service
@RequiredArgsConstructor
public class FlashscoreTeamNamesService {

    private static final int DAY_FEED_PAST_DAYS = 7;
    private static final int DAY_FEED_FUTURE_DAYS = 21;

    private final FlashscoreHttpClient httpClient;
    private final FlashscoreDayFeedParser dayFeedParser;
    private final FlashscoreProperties properties;

    public List<String> fetchTeamNames(String leagueCodeRaw) {
        League.LeagueCode leagueCode = FlashscoreFullMatchResolver.parseLeagueCode(leagueCodeRaw);
        FlashscoreProperties.LeagueConfig config = requireLeagueConfig(leagueCode);
        LocalDate today = LocalDate.now(ZoneOffset.UTC);
        Set<String> unique = new LinkedHashSet<>();
        for (int offset = -DAY_FEED_PAST_DAYS; offset <= DAY_FEED_FUTURE_DAYS; offset++) {
            LocalDate day = today.plusDays(offset);
            String feed = httpClient.fetchDayFootballFeed(day);
            FlashscoreParsedDayPage page = dayFeedParser.parse(feed, day);
            unique.addAll(extractTeamNames(page, config));
        }
        if (unique.isEmpty()) {
            throw new BadRequestException("flashscoreTeamNamesEmpty");
        }
        return new ArrayList<>(unique);
    }

    FlashscoreProperties.LeagueConfig requireLeagueConfig(League.LeagueCode leagueCode) {
        FlashscoreProperties.LeagueConfig config = properties.getLeagues() != null
                ? properties.getLeagues().get(leagueCode.name())
                : null;
        if (config == null || config.getTournamentPath() == null || config.getTournamentPath().isBlank()) {
            throw new BadRequestException("flashscoreTournamentNotConfigured");
        }
        return config;
    }

    static List<String> extractTeamNames(FlashscoreParsedDayPage page, FlashscoreProperties.LeagueConfig config) {
        if (page == null || page.getCompetitions() == null || config == null) {
            return List.of();
        }
        Set<String> unique = new LinkedHashSet<>();
        for (FlashscoreParsedDayPage.CompetitionBlock block : page.getCompetitions()) {
            if (!competitionMatchesLeague(block, config)) {
                continue;
            }
            if (block.getMatches() == null) {
                continue;
            }
            for (FlashscoreParsedDayPage.Match match : block.getMatches()) {
                addName(unique, match.getHomeName());
                addName(unique, match.getAwayName());
            }
        }
        unique.removeIf(FlashscoreTeamNamesService::isCountryLikeName);
        return new ArrayList<>(unique);
    }

    static boolean competitionMatchesLeague(
            FlashscoreParsedDayPage.CompetitionBlock block,
            FlashscoreProperties.LeagueConfig config
    ) {
        if (block == null || config == null) {
            return false;
        }
        if (config.getStageId() != null
                && !config.getStageId().isBlank()
                && config.getStageId().equalsIgnoreCase(block.getStageId())) {
            return true;
        }
        return FlashscoreDayFeedParser.competitionMatchesFilter(block, config.getTitleContains());
    }

    private static boolean isCountryLikeName(String name) {
        if (name == null) {
            return true;
        }
        String lower = name.toLowerCase();
        return lower.equals("англия")
                || lower.equals("германия")
                || lower.equals("england")
                || lower.equals("germany");
    }

    private static void addName(Set<String> unique, String raw) {
        if (raw == null) {
            return;
        }
        String name = raw.replace('\u00a0', ' ').trim();
        if (!name.isEmpty()) {
            unique.add(name);
        }
    }
}
