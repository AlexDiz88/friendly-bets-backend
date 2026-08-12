package net.friendly_bets.liveresult;

import net.friendly_bets.exceptions.BadRequestException;
import net.friendly_bets.providers.standings.StandingRowSnapshot;
import net.friendly_bets.providers.standings.StandingZoneRuleSnapshot;
import net.friendly_bets.providers.standings.StandingsTableSnapshot;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class LiveresultStandingsParser {

    private static final Pattern RANK_PATTERN = Pattern.compile("(\\d+)\\s*\\.");
    private static final Pattern GOALS_PATTERN = Pattern.compile("(\\d+)\\s*-\\s*(\\d+)");

    public StandingsTableSnapshot parse(String html, String sourceUrl) {
        if (html == null || html.isBlank()) {
            throw new BadRequestException("liveresultStandingsEmpty");
        }
        Document doc = Jsoup.parse(html);
        Element table = doc.selectFirst("table.maintbl.standings-table");
        if (table == null) {
            throw new BadRequestException("liveresultStandingsEmpty");
        }

        Map<String, StandingZoneRuleSnapshot> zoneRules = parseLegend(doc);
        List<StandingRowSnapshot> rows = new ArrayList<>();
        Elements bodyRows = table.select("tbody > tr");
        for (Element row : bodyRows) {
            StandingRowSnapshot parsed = parseRow(row, zoneRules);
            if (parsed != null) {
                rows.add(parsed);
            }
        }
        if (rows.isEmpty()) {
            throw new BadRequestException("liveresultStandingsEmpty");
        }

        return StandingsTableSnapshot.builder()
                .sourceUrl(sourceUrl)
                .rows(rows)
                .zoneRules(new ArrayList<>(zoneRules.values()))
                .build();
    }

    private Map<String, StandingZoneRuleSnapshot> parseLegend(Document doc) {
        Map<String, StandingZoneRuleSnapshot> rules = new LinkedHashMap<>();
        Elements legendItems = doc.select("ul.list-unstyled > li");
        for (Element item : legendItems) {
            Element swatch = item.selectFirst("i[class*=bg-]");
            if (swatch == null) {
                continue;
            }
            String cssClass = extractZoneCssClass(swatch.classNames());
            if (cssClass == null) {
                continue;
            }
            String label = extractLegendLabel(item.text());
            if (label == null || label.isBlank()) {
                continue;
            }
            String code = zoneCodeFromCssClass(cssClass);
            rules.putIfAbsent(code, StandingZoneRuleSnapshot.builder()
                    .code(code)
                    .label(label.trim())
                    .cssClass(cssClass)
                    .build());
        }
        return rules;
    }

    private StandingRowSnapshot parseRow(Element row, Map<String, StandingZoneRuleSnapshot> zoneRules) {
        Element rankCell = row.selectFirst("td.num");
        Element nameCell = row.selectFirst("td.name");
        if (rankCell == null || nameCell == null) {
            return null;
        }
        Element teamLink = nameCell.selectFirst("a[href]");
        String teamName = teamLink != null ? teamLink.text() : nameCell.text();
        if (teamName == null || teamName.isBlank()) {
            return null;
        }

        int rank = parseRank(rankCell.text());
        String zoneCss = extractZoneCssClass(rankCell.classNames());
        String zoneCode = zoneCss != null ? zoneCodeFromCssClass(zoneCss) : null;
        if (zoneCode != null && zoneCss != null) {
            String title = rankCell.hasAttr("title") ? rankCell.attr("title").trim() : null;
            zoneRules.putIfAbsent(zoneCode, StandingZoneRuleSnapshot.builder()
                    .code(zoneCode)
                    .label(title != null && !title.isBlank() ? title : zoneCode)
                    .cssClass(zoneCss)
                    .build());
        }

        String logoUrl = null;
        Element logoImg = row.selectFirst("td.team-icon img[src]");
        if (logoImg != null) {
            logoUrl = logoImg.attr("abs:src");
            if (logoUrl == null || logoUrl.isBlank()) {
                logoUrl = logoImg.attr("src");
            }
        }

        List<Element> scoreCells = row.select("td.score");
        if (scoreCells.size() < 6) {
            return null;
        }
        int played = parseInt(scoreCells.get(0).text());
        int wins = parseInt(scoreCells.get(1).text());
        int draws = parseInt(scoreCells.get(2).text());
        int losses = parseInt(scoreCells.get(3).text());
        int[] goals = parseGoals(scoreCells.get(4).text());
        int points = parseInt(scoreCells.get(5).text());

        return StandingRowSnapshot.builder()
                .rank(rank)
                .externalTeamName(teamName.trim())
                .logoUrl(logoUrl)
                .played(played)
                .wins(wins)
                .draws(draws)
                .losses(losses)
                .goalsFor(goals[0])
                .goalsAgainst(goals[1])
                .goalDifference(goals[0] - goals[1])
                .points(points)
                .zoneCode(zoneCode)
                .build();
    }

    private static String extractLegendLabel(String text) {
        if (text == null) {
            return null;
        }
        int dash = text.indexOf('—');
        if (dash < 0) {
            dash = text.indexOf('-');
        }
        if (dash >= 0 && dash + 1 < text.length()) {
            return text.substring(dash + 1).trim();
        }
        return text.trim();
    }

    private static String extractZoneCssClass(Iterable<String> classNames) {
        for (String className : classNames) {
            if (className != null && className.startsWith("bg-")) {
                return className;
            }
        }
        return null;
    }

    static String zoneCodeFromCssClass(String cssClass) {
        if (cssClass == null || cssClass.isBlank()) {
            return null;
        }
        return cssClass.trim().toLowerCase(Locale.ROOT);
    }

    private static int parseRank(String text) {
        if (text == null) {
            return 0;
        }
        Matcher matcher = RANK_PATTERN.matcher(text.trim());
        if (matcher.find()) {
            return Integer.parseInt(matcher.group(1));
        }
        return parseInt(text);
    }

    private static int[] parseGoals(String text) {
        if (text == null) {
            return new int[] {0, 0};
        }
        Matcher matcher = GOALS_PATTERN.matcher(text.trim());
        if (matcher.find()) {
            return new int[] {
                    Integer.parseInt(matcher.group(1)),
                    Integer.parseInt(matcher.group(2))
            };
        }
        return new int[] {0, 0};
    }

    private static int parseInt(String text) {
        if (text == null || text.isBlank()) {
            return 0;
        }
        String digits = text.replaceAll("[^0-9-]", "").trim();
        if (digits.isEmpty()) {
            return 0;
        }
        return Integer.parseInt(digits);
    }
}
