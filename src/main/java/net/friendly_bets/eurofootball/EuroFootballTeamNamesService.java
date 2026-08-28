package net.friendly_bets.eurofootball;

import lombok.RequiredArgsConstructor;
import net.friendly_bets.eurofootball.config.EuroFootballProperties;
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
public class EuroFootballTeamNamesService {

    private final EuroFootballHttpClient httpClient;
    private final EuroFootballProperties properties;

    public List<String> fetchTeamNames(String leagueCodeRaw) {
        League.LeagueCode leagueCode = Soccer365TeamNamesService.parseLeagueCode(leagueCodeRaw);
        if (!EuroFootballLeagueSupport.supported().contains(leagueCode)) {
            throw new BadRequestException("euroFootballLeagueNotSupported");
        }
        String path = properties.getLeaguePaths() != null
                ? properties.getLeaguePaths().get(leagueCode.name())
                : null;
        if (path == null || path.isBlank()) {
            throw new BadRequestException("euroFootballLeagueNotSupported");
        }
        String html = httpClient.fetchLeagueTablesHtml(path);
        List<String> names = parseTeamNamesFromTablesHtml(html);
        if (names.isEmpty()) {
            throw new BadRequestException("euroFootballTeamNamesEmpty");
        }
        return names;
    }

    /**
     * League standings widget is a direct child of {@code div.block}.
     * The site-wide dropdown ({@code block__select-turnir-tables}) is ignored —
     * on CL/LE before group stage it would otherwise return another league (e.g. RPL).
     */
    static List<String> parseTeamNamesFromTablesHtml(String html) {
        if (html == null || html.isBlank()) {
            return List.of();
        }
        Document doc = Jsoup.parse(html);
        Element widget = doc.selectFirst("div.block > div.tournament-tables-widget");
        if (widget == null) {
            return List.of();
        }
        Element table = widget.selectFirst(
                "div.tournament-tables-widget__table[data-tab-content=main] table.table-turnir"
        );
        if (table == null) {
            table = widget.selectFirst("table.table-turnir");
        }
        if (table == null) {
            return List.of();
        }
        Elements links = table.select("a[href^=/team/]");
        Set<String> unique = new LinkedHashSet<>();
        for (Element el : links) {
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
