package net.friendly_bets.ruscore;

import lombok.RequiredArgsConstructor;
import net.friendly_bets.exceptions.BadRequestException;
import net.friendly_bets.models.League;
import net.friendly_bets.ruscore.config.RuscoreProperties;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class RuscoreTeamNamesService {

    private final RuscoreHttpClient httpClient;
    private final RuscoreProperties properties;

    public List<String> fetchTeamNames(String leagueCodeRaw) {
        League.LeagueCode leagueCode = RuscoreFullMatchResolver.parseLeagueCode(leagueCodeRaw);
        Integer seasonId = requireSeasonId(leagueCode);
        String slug = requireTournamentSlug(leagueCode);
        String html = httpClient.fetchTournamentCalendarHtml(slug, seasonId);
        List<String> names = parseTeamNamesFromCalendar(html);
        if (names.isEmpty()) {
            throw new BadRequestException("ruscoreTeamNamesEmpty");
        }
        return names;
    }

    int requireSeasonId(League.LeagueCode leagueCode) {
        Map<String, Integer> ids = properties.getSeasonIds();
        Integer id = ids != null ? ids.get(leagueCode.name()) : null;
        if (id == null || id <= 0) {
            throw new BadRequestException("ruscoreTournamentNotConfigured");
        }
        return id;
    }

    String requireTournamentSlug(League.LeagueCode leagueCode) {
        Map<String, String> slugs = properties.getTournamentSlugs();
        String slug = slugs != null ? slugs.get(leagueCode.name()) : null;
        if (slug == null || slug.isBlank()) {
            throw new BadRequestException("ruscoreTournamentNotConfigured");
        }
        return slug.trim();
    }

    static List<String> parseTeamNamesFromCalendar(String html) {
        if (html == null || html.isBlank()) {
            return List.of();
        }
        Document doc = Jsoup.parse(html);
        Set<String> unique = new LinkedHashSet<>();
        Elements homes = doc.select("[data-test-id=player-title-home]");
        Elements aways = doc.select("[data-test-id=player-title-away]");
        for (Element el : homes) {
            addName(unique, el.text());
        }
        for (Element el : aways) {
            addName(unique, el.text());
        }
        return new ArrayList<>(unique);
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
