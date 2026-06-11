package net.friendly_bets.fourscore;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.stereotype.Component;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

@Component
public class FourScoreMatchListParser {

    private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("H:mm");

    public List<FourScoreListMatch> parse(String html, Set<FourScoreLeagueSection> sections) {
        if (html == null || html.isBlank() || sections == null || sections.isEmpty()) {
            return List.of();
        }
        Document doc = Jsoup.parse(html);
        List<FourScoreListMatch> result = new ArrayList<>();
        for (Element leagueBlock : doc.select("div.lg")) {
            FourScoreLeagueSection section = resolveSection(leagueBlock);
            if (section == null || !sections.contains(section)) {
                continue;
            }
            for (Element block : leagueBlock.select("div.lg__block")) {
                FourScoreListMatch match = parseBlock(block, section);
                if (match != null) {
                    result.add(match);
                }
            }
        }
        return result;
    }

    public List<FourScoreListMatch> parse(String html, FourScoreLeagueSection section) {
        return parse(html, EnumSet.of(section));
    }

    private static FourScoreLeagueSection resolveSection(Element leagueBlock) {
        Element nameLink = leagueBlock.selectFirst("a.lg__name");
        if (nameLink == null) {
            return null;
        }
        String name = nameLink.text().trim();
        for (FourScoreLeagueSection section : FourScoreLeagueSection.values()) {
            if (section.leagueName().equalsIgnoreCase(name)) {
                return section;
            }
        }
        return null;
    }

    private static FourScoreListMatch parseBlock(Element block, FourScoreLeagueSection section) {
        Element link = block.selectFirst("a.lg__cnt");
        if (link == null) {
            return null;
        }
        Elements teams = link.select("span.lg__team");
        if (teams.size() < 2) {
            return null;
        }
        String path = link.attr("href");
        String slug = slugFromPath(path);
        Element statusEl = block.selectFirst("div.lg__status");
        Element timeEl = block.selectFirst("span.lg__time");
        Integer homeScore = parseScore(block.selectFirst("span.lg__score-localteam"));
        Integer awayScore = parseScore(block.selectFirst("span.lg__score-visitorteam"));
        Long externalId = null;
        String dataId = block.attr("data-id");
        if (!dataId.isBlank()) {
            try {
                externalId = Long.parseLong(dataId);
            } catch (NumberFormatException ignored) {
            }
        }
        LocalTime kickoff = null;
        if (timeEl != null && !timeEl.text().isBlank()) {
            try {
                kickoff = LocalTime.parse(timeEl.text().trim(), TIME);
            } catch (Exception ignored) {
            }
        }
        return FourScoreListMatch.builder()
                .section(section)
                .eventPath(path)
                .eventSlug(slug)
                .homeTeamName(teams.get(0).text().trim())
                .awayTeamName(teams.get(1).text().trim())
                .statusText(statusEl != null ? statusEl.text().trim() : null)
                .homeScore(homeScore)
                .awayScore(awayScore)
                .kickoffTime(kickoff)
                .externalEventId(externalId)
                .build();
    }

    private static Integer parseScore(Element scoreEl) {
        if (scoreEl == null || scoreEl.text().isBlank()) {
            return null;
        }
        try {
            return Integer.parseInt(scoreEl.text().trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    static String slugFromPath(String path) {
        if (path == null || path.isBlank()) {
            return "";
        }
        String trimmed = path.trim();
        if (trimmed.startsWith("/")) {
            trimmed = trimmed.substring(1);
        }
        if (trimmed.endsWith("/")) {
            trimmed = trimmed.substring(0, trimmed.length() - 1);
        }
        if (trimmed.startsWith("events/")) {
            trimmed = trimmed.substring("events/".length());
        }
        return trimmed;
    }
}
