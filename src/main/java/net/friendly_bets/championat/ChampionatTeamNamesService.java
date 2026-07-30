package net.friendly_bets.championat;

import lombok.RequiredArgsConstructor;
import net.friendly_bets.championat.config.ChampionatProperties;
import net.friendly_bets.exceptions.BadRequestException;
import net.friendly_bets.models.League;
import net.friendly_bets.soccer365.Soccer365TeamNamesService;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class ChampionatTeamNamesService {

    private final ChampionatHttpClient httpClient;
    private final ChampionatProperties properties;

    public List<String> fetchTeamNames(String leagueCodeRaw) {
        League.LeagueCode leagueCode = Soccer365TeamNamesService.parseLeagueCode(leagueCodeRaw);
        if (!ChampionatLeagueSupport.supported().contains(leagueCode)) {
            throw new BadRequestException("championatLeagueNotSupported");
        }
        String path = properties.getTournamentPaths() != null
                ? properties.getTournamentPaths().get(leagueCode.name())
                : null;
        if (path == null || path.isBlank()) {
            throw new BadRequestException("championatTournamentNotConfigured");
        }
        String html = httpClient.fetchTournamentTableHtml(path);
        List<String> names = parseTeamNamesFromTable(html);
        if (names.isEmpty()) {
            throw new BadRequestException("championatTeamNamesEmpty");
        }
        return names;
    }

    static List<String> parseTeamNamesFromTable(String html) {
        if (html == null || html.isBlank()) {
            return List.of();
        }
        Document doc = Jsoup.parse(html);
        Elements items = doc.select(".table-item__name");
        Set<String> unique = new LinkedHashSet<>();
        for (Element el : items) {
            String name = el.text();
            if (name != null) {
                name = name.trim();
                if (!name.isEmpty()) {
                    unique.add(name);
                }
            }
        }
        return new ArrayList<>(unique);
    }
}
