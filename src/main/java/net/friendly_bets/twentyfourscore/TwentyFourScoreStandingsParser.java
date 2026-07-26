package net.friendly_bets.twentyfourscore;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class TwentyFourScoreStandingsParser {

    private static final Pattern DATA_KEY = Pattern.compile("data_key\"\\s*:\\s*\"([^\"]+)\"");

    public String extractDataKey(String standingsShellHtml) {
        if (standingsShellHtml == null || standingsShellHtml.isBlank()) {
            return null;
        }
        Matcher matcher = DATA_KEY.matcher(standingsShellHtml);
        if (!matcher.find()) {
            return null;
        }
        String key = matcher.group(1);
        return key != null && !key.isBlank() ? key.trim() : null;
    }

    public List<String> parseTeamNames(String standingsDataHtml) {
        Set<String> names = new LinkedHashSet<>();
        if (standingsDataHtml == null || standingsDataHtml.isBlank()) {
            return List.of();
        }
        Document doc = Jsoup.parse(standingsDataHtml);
        Elements links = doc.select("a[href^=/football/team/]");
        for (Element link : links) {
            String name = link.text() != null ? link.text().trim() : "";
            if (!name.isEmpty()) {
                names.add(name);
            }
        }
        return new ArrayList<>(names);
    }
}
