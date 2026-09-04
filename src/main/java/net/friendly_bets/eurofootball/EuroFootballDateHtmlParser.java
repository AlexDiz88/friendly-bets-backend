package net.friendly_bets.eurofootball;

import net.friendly_bets.providers.live.LiveMatchSnapshot;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Parses euro-football.ru online day pages ({@code /online/today}, {@code /yesterday}, …).
 * Only the main schedule feed ({@code .match-schedule__item}) — sidebar widgets
 * ({@code .match-online__item}, {@code .liveresult__*}) are excluded.
 */
@Component
public class EuroFootballDateHtmlParser {

    private static final Pattern ONLINE_LEAGUE_PATH = Pattern.compile("/online/([^/\"]+)(?:/([^/\"]+))?");

    public EuroFootballParsedDatePage parse(String html) {
        Document doc = Jsoup.parse(html != null ? html : "");
        List<EuroFootballParsedDatePage.CompetitionBlock> blocks = new ArrayList<>();
        for (Element section : doc.select(".match-schedule__item")) {
            if (section.closest(".liveresult") != null) {
                continue;
            }
            EuroFootballParsedDatePage.CompetitionBlock block = parseCompetition(section);
            if (block != null && !block.getMatches().isEmpty()) {
                blocks.add(block);
            }
        }
        return EuroFootballParsedDatePage.builder().competitions(blocks).build();
    }

    private static EuroFootballParsedDatePage.CompetitionBlock parseCompetition(Element section) {
        String slug = null;
        String parentSlug = null;
        String title = null;
        Element header = section.selectFirst(".match-schedule__item-header");
        if (header != null) {
            Elements titleLinks = header.select("a.match-schedule__item-header__title");
            if (titleLinks.size() >= 2) {
                parentSlug = slugFromHref(titleLinks.get(0).attr("href"), true);
                slug = slugFromHref(titleLinks.get(1).attr("href"), false);
                title = text(titleLinks.get(0)) + " " + text(titleLinks.get(1));
            } else if (titleLinks.size() == 1) {
                slug = slugFromHref(titleLinks.get(0).attr("href"), false);
                title = text(titleLinks.get(0));
            }
        }
        List<EuroFootballParsedDatePage.MatchRow> rows = new ArrayList<>();
        for (Element item : section.select(".match-schedule__item-container .match-online-list__item")) {
            EuroFootballParsedDatePage.MatchRow row = parseMatchRow(item);
            if (row != null) {
                rows.add(row);
            }
        }
        if (rows.isEmpty()) {
            return null;
        }
        return EuroFootballParsedDatePage.CompetitionBlock.builder()
                .title(title)
                .slug(slug)
                .parentSlug(parentSlug)
                .matches(rows)
                .build();
    }

    private static EuroFootballParsedDatePage.MatchRow parseMatchRow(Element item) {
        if (item == null) {
            return null;
        }
        Element homeEl = item.selectFirst(".team1name span");
        Element awayEl = item.selectFirst(".team2name span");
        if (homeEl == null || awayEl == null) {
            return null;
        }
        String home = text(homeEl);
        String away = text(awayEl);
        if (home.isBlank() || away.isBlank()) {
            return null;
        }
        String dataStatus = item.attr("data-status");
        String statusText = text(item.selectFirst(".match-online-list__item-status"));
        String mappedStatus = EuroFootballDateJsonParser.mapStatus(dataStatus, statusCodeFromData(item), statusText);
        String rawMinute = EuroFootballDateJsonParser.extractMinuteLabel(mappedStatus, statusText);
        String fullTime = scoreFromItem(item, mappedStatus);
        String scoreText = fullTime == null ? null : fullTime.replace(":", " : ");
        String externalId = item.hasAttr("data-match-id") ? item.attr("data-match-id") : null;
        return EuroFootballParsedDatePage.MatchRow.builder()
                .externalMatchId(externalId)
                .homeName(home)
                .awayName(away)
                .scoreText(scoreText)
                .snapshot(new LiveMatchSnapshot(mappedStatus, rawMinute, fullTime, null))
                .build();
    }

    private static int statusCodeFromData(Element item) {
        String status = item.attr("data-status");
        if ("finished".equalsIgnoreCase(status)) {
            return 9;
        }
        if ("live".equalsIgnoreCase(status)) {
            return 3;
        }
        if ("soon".equalsIgnoreCase(status)) {
            return 0;
        }
        return -1;
    }

    private static String scoreFromItem(Element item, String mappedStatus) {
        if (!EuroFootballDateJsonParser.scoreAllowed(mappedStatus)) {
            return null;
        }
        Elements goals = item.select(".item-score-link .goal-team-block");
        if (goals.size() < 2) {
            return null;
        }
        String home = text(goals.get(0));
        String away = text(goals.get(1));
        if (home.isBlank() || away.isBlank()) {
            return null;
        }
        return home + ":" + away;
    }

    static String slugFromHref(String href, boolean parentOnly) {
        if (href == null || href.isBlank()) {
            return null;
        }
        Matcher matcher = ONLINE_LEAGUE_PATH.matcher(href.trim());
        if (!matcher.find()) {
            return null;
        }
        String first = matcher.group(1);
        String second = matcher.group(2);
        if (parentOnly) {
            return first;
        }
        return second != null && !second.isBlank() ? second : first;
    }

    private static String text(Element el) {
        if (el == null) {
            return "";
        }
        String text = el.text();
        return text == null ? "" : text.trim();
    }
}
