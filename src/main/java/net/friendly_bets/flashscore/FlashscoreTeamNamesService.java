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

/**
 * Team names for alias autobind — canonical English FH/FK labels from the league
 * tournament page feed (same strings as day feed / FULL_MATCH), scoped by stage id.
 */
@Service
@RequiredArgsConstructor
public class FlashscoreTeamNamesService {

    private static final Pattern EMBEDDED_FEED_BLOCK = Pattern.compile("data: `([^`]+)`");

    private final FlashscoreHttpClient httpClient;
    private final FlashscoreProperties properties;

    public List<String> fetchTeamNames(String leagueCodeRaw) {
        League.LeagueCode leagueCode = FlashscoreFullMatchResolver.parseLeagueCode(leagueCodeRaw);
        FlashscoreProperties.LeagueConfig config = requireLeagueConfig(leagueCode);
        String html = httpClient.fetchTournamentPageHtml(
                config.getTournamentPath(),
                properties.getTeamNamesBaseUrl()
        );
        List<String> names = parseTeamNamesFromTournamentHtml(html, config.getStageId());
        if (names.isEmpty()) {
            throw new BadRequestException("flashscoreTeamNamesEmpty");
        }
        return names;
    }

    FlashscoreProperties.LeagueConfig requireLeagueConfig(League.LeagueCode leagueCode) {
        FlashscoreProperties.LeagueConfig config = properties.getLeagues() != null
                ? properties.getLeagues().get(leagueCode.name())
                : null;
        if (config == null || config.getTournamentPath() == null || config.getTournamentPath().isBlank()) {
            throw new BadRequestException("flashscoreTournamentNotConfigured");
        }
        if (config.getStageId() == null || config.getStageId().isBlank()) {
            throw new BadRequestException("flashscoreTournamentNotConfigured");
        }
        return config;
    }

    static List<String> parseTeamNamesFromTournamentHtml(String html, String stageId) {
        if (html == null || html.isBlank() || stageId == null || stageId.isBlank()) {
            return List.of();
        }
        Set<String> unique = new LinkedHashSet<>();
        Matcher matcher = EMBEDDED_FEED_BLOCK.matcher(html);
        while (matcher.find()) {
            unique.addAll(extractTeamNamesFromFeedBlock(matcher.group(1), stageId));
        }
        unique.removeIf(FlashscoreTeamNamesService::isCountryLikeName);
        return new ArrayList<>(unique);
    }

    static Set<String> extractTeamNamesFromFeedBlock(String feedBlock, String stageId) {
        Set<String> unique = new LinkedHashSet<>();
        String currentStageId = null;
        for (String record : FlashscoreFeedSupport.splitRecords(feedBlock)) {
            Map<String, String> fields = FlashscoreFeedSupport.parseRecord(record);
            String za = fields.get("ZA");
            if (za != null) {
                currentStageId = fields.get("ZC");
                continue;
            }
            if (!stageId.equalsIgnoreCase(currentStageId != null ? currentStageId : "")) {
                continue;
            }
            if (fields.containsKey("AA")) {
                addName(unique, fields.get("FH"));
                addName(unique, fields.get("FK"));
                continue;
            }
            if (fields.containsKey("TN")) {
                addName(unique, fields.get("TN"));
            }
        }
        return unique;
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
