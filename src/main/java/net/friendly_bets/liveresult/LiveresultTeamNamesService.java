package net.friendly_bets.liveresult;

import lombok.RequiredArgsConstructor;
import net.friendly_bets.exceptions.BadRequestException;
import net.friendly_bets.models.League;
import net.friendly_bets.models.Season;
import net.friendly_bets.providers.standings.StandingsTableSnapshot;
import net.friendly_bets.liveresult.config.LiveresultProperties;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class LiveresultTeamNamesService {

    private final LiveresultProperties properties;
    private final LiveresultHttpClient httpClient;
    private final LiveresultStandingsParser standingsParser;

    public List<String> fetchTeamNames(String leagueCodeRaw) {
        League.LeagueCode leagueCode = parseLeagueCode(leagueCodeRaw);
        String path = requireStandingsPath(leagueCode);
        String base = properties.getBaseUrl().replaceAll("/+$", "");
        String url = base + (path.startsWith("/") ? path : "/" + path);
        String html = httpClient.fetchStandingsHtml(path);
        StandingsTableSnapshot snapshot = standingsParser.parse(html, url);
        List<String> names = snapshot.getRows().stream()
                .map(row -> row.getExternalTeamName())
                .filter(name -> name != null && !name.isBlank())
                .distinct()
                .toList();
        if (names.isEmpty()) {
            throw new BadRequestException("liveresultTeamNamesEmpty");
        }
        return names;
    }

    String requireStandingsPath(League.LeagueCode leagueCode) {
        Map<String, String> paths = properties.getStandingsPaths();
        if (paths == null || paths.isEmpty()) {
            throw new BadRequestException("liveresultStandingsNotConfigured");
        }
        String path = paths.get(leagueCode.name());
        if (path == null || path.isBlank()) {
            throw new BadRequestException("liveresultStandingsNotConfigured");
        }
        return path.trim();
    }

    static League.LeagueCode parseLeagueCode(String leagueCodeRaw) {
        if (leagueCodeRaw == null || leagueCodeRaw.isBlank()) {
            throw new BadRequestException("leagueCodeRequired");
        }
        try {
            return League.LeagueCode.valueOf(leagueCodeRaw.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new BadRequestException("invalidLeagueCode");
        }
    }
}
