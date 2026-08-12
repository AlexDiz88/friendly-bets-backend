package net.friendly_bets.flashscore;

import lombok.RequiredArgsConstructor;
import net.friendly_bets.exceptions.BadRequestException;
import net.friendly_bets.flashscore.config.FlashscoreProperties;
import net.friendly_bets.models.League;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
public class FlashscoreTeamNamesService {

    /** Canonical team labels for alias autobind (FH/FK), not localized CX/AF. */
    private static final Pattern FEED_NAME_FIELD = Pattern.compile(
            "(?:FH|FK)÷([^¬~{}]+)"
    );

    private final FlashscoreHttpClient httpClient;
    private final FlashscoreProperties properties;

    public List<String> fetchTeamNames(String leagueCodeRaw) {
        League.LeagueCode leagueCode = FlashscoreFullMatchResolver.parseLeagueCode(leagueCodeRaw);
        FlashscoreProperties.LeagueConfig config = requireLeagueConfig(leagueCode);
        String html = httpClient.fetchTournamentPageHtml(config.getTournamentPath());
        List<String> names = parseTeamNamesFromTournamentHtml(html);
        if (names.isEmpty()) {
            throw new BadRequestException("flashscoreTeamNamesEmpty");
        }
        return names;
    }

    FlashscoreProperties.LeagueConfig requireLeagueConfig(League.LeagueCode leagueCode) {
        Map<String, FlashscoreProperties.LeagueConfig> leagues = properties.getLeagues();
        FlashscoreProperties.LeagueConfig config = leagues != null ? leagues.get(leagueCode.name()) : null;
        if (config == null || config.getTournamentPath() == null || config.getTournamentPath().isBlank()) {
            throw new BadRequestException("flashscoreTournamentNotConfigured");
        }
        return config;
    }

    static List<String> parseTeamNamesFromTournamentHtml(String html) {
        if (html == null || html.isBlank()) {
            return List.of();
        }
        Set<String> unique = new LinkedHashSet<>();
        Matcher matcher = FEED_NAME_FIELD.matcher(html);
        while (matcher.find()) {
            addName(unique, matcher.group(1));
        }
        unique.removeIf(FlashscoreTeamNamesService::isCountryLikeName);
        return new ArrayList<>(unique);
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
