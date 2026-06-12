package net.friendly_bets.fourscore;

import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.jsoup.Jsoup;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class FourScoreEventPageParser {

    private static final DateTimeFormatter KICKOFF = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm");
    private static final Pattern PERIOD_SCORE = Pattern.compile("(\\d+)\\s*:\\s*(\\d+)");

    public FourScoreEventDetails parse(String html, String eventPath, FourScoreLeagueSection section) {
        if (html == null || html.isBlank()) {
            return null;
        }
        Document doc = Jsoup.parse(html);
        Element homeEl = doc.selectFirst("div.event__c[itemprop=homeTeam] span.event__c-name");
        Element awayEl = doc.selectFirst("div.event__c[itemprop=awayTeam] span.event__c-name");
        if (homeEl == null || awayEl == null) {
            return null;
        }
        String homeName = homeEl.text().trim();
        String awayName = awayEl.text().trim();
        Element statusEl = doc.selectFirst("div.event__status");
        String statusText = statusEl != null ? statusEl.text().trim() : null;

        Integer homeHeader = parseInt(doc.selectFirst("div.event__score-i[data-team=localteam]"));
        Integer awayHeader = parseInt(doc.selectFirst("div.event__score-i[data-team=visitorteam]"));

        String firstHalf = null;
        String secondHalf = null;
        String extraTime = null;
        String penalty = null;
        LocalDateTime kickoffAt = null;

        for (Element textBlock : doc.select("div.event__text")) {
            Element span = textBlock.selectFirst("span");
            if (span == null) {
                String plain = textBlock.text().trim();
                if (kickoffAt == null) {
                    kickoffAt = parseKickoff(plain);
                }
                continue;
            }
            String label = span.text().trim().toLowerCase(Locale.ROOT);
            String scoreText = scoreFromBlock(textBlock);
            if (label.startsWith("1-й тайм")) {
                firstHalf = scoreText;
            } else if (label.startsWith("2-й тайм")) {
                secondHalf = scoreText;
            } else if (label.startsWith("доп")) {
                extraTime = scoreText;
            } else if (label.startsWith("пенальти")) {
                penalty = scoreText;
            }
        }

        String slug = FourScoreMatchListParser.slugFromPath(eventPath);
        return FourScoreEventDetails.builder()
                .eventSlug(slug)
                .eventPath(eventPath)
                .homeTeamName(homeName)
                .awayTeamName(awayName)
                .statusText(statusText)
                .headerHomeScore(homeHeader)
                .headerAwayScore(awayHeader)
                .firstHalfScore(firstHalf)
                .secondHalfScore(secondHalf)
                .extraTimeScore(extraTime)
                .penaltyScore(penalty)
                .kickoffAt(kickoffAt)
                .section(section)
                .build();
    }

    private static String scoreFromBlock(Element textBlock) {
        Element bold = textBlock.selectFirst("b");
        if (bold != null && !bold.text().isBlank()) {
            return normalizeScore(bold.text());
        }
        Matcher matcher = PERIOD_SCORE.matcher(textBlock.text());
        if (matcher.find()) {
            return matcher.group(1) + ":" + matcher.group(2);
        }
        return null;
    }

    private static String normalizeScore(String raw) {
        Matcher matcher = PERIOD_SCORE.matcher(raw);
        if (!matcher.find()) {
            return null;
        }
        return matcher.group(1) + ":" + matcher.group(2);
    }

    private static Integer parseInt(Element el) {
        if (el == null || el.text().isBlank()) {
            return null;
        }
        try {
            return Integer.parseInt(el.text().trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private static LocalDateTime parseKickoff(String plain) {
        if (plain == null || plain.isBlank()) {
            return null;
        }
        try {
            return LocalDateTime.parse(plain.trim(), KICKOFF);
        } catch (Exception e) {
            return null;
        }
    }
}
