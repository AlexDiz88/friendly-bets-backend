package net.friendly_bets.twentyfourscore;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class TwentyFourScoreScheduleParser {

    private static final Pattern MATCH_PATH = Pattern.compile("/football/match/(\\d+)-([^/]+)/?");
    private static final Pattern SCORE_PAIR = Pattern.compile("(\\d+)\\s*:\\s*(\\d+)");
    private static final Pattern EXTRA_PEN = Pattern.compile(
            "дв\\s*(\\d+:\\d+).*?пен\\s*(\\d+:\\d+)",
            Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE
    );
    private static final DateTimeFormatter KICKOFF_TIME = DateTimeFormatter.ofPattern("HH:mm");

    public List<TwentyFourScoreListMatch> parseDailyPage(String html, LocalDate date, String competitionMarker) {
        if (html == null || html.isBlank()) {
            return List.of();
        }
        Document doc = Jsoup.parse(html);
        List<TwentyFourScoreListMatch> matches = new ArrayList<>();
        for (Element header : doc.select("th.champheader")) {
            Element link = header.selectFirst("a[href*=" + competitionMarker + "]");
            if (link == null) {
                continue;
            }
            Element row = header.parent();
            while (row != null) {
                row = row.nextElementSibling();
                if (row == null) {
                    break;
                }
                if (row.selectFirst("th.champheader") != null) {
                    break;
                }
                TwentyFourScoreListMatch parsed = parseRow(row, date);
                if (parsed != null) {
                    matches.add(parsed);
                }
            }
        }
        return matches;
    }

    private TwentyFourScoreListMatch parseRow(Element row, LocalDate date) {
        Element scoreLink = row.selectFirst("td.score a[href*=/football/match/]");
        if (scoreLink == null) {
            return null;
        }
        String href = scoreLink.attr("href");
        Matcher pathMatcher = MATCH_PATH.matcher(href);
        if (!pathMatcher.find()) {
            return null;
        }
        long matchId = Long.parseLong(pathMatcher.group(1));
        String homeName = textOrNull(row.selectFirst("td.team span.tm1"));
        String awayName = textOrNull(row.selectFirst("td.team span.tm2"));
        if (homeName == null || awayName == null) {
            return null;
        }
        LocalTime kickoff = parseKickoffTime(textOrNull(row.selectFirst("td.time")));
        ScoreParts scores = parseScoreText(scoreLink.text());
        return TwentyFourScoreListMatch.builder()
                .externalMatchId(matchId)
                .matchPath(href)
                .homeTeamName(homeName)
                .awayTeamName(awayName)
                .matchDate(date)
                .kickoffTime(kickoff)
                .fullTimeScore(scores.fullTime())
                .firstHalfScore(scores.firstHalf())
                .extraTimeScore(scores.extraTime())
                .penaltyScore(scores.penalty())
                .statusText(scores.fullTime() != null ? "Завершен" : null)
                .build();
    }

    static ScoreParts parseScoreText(String raw) {
        if (raw == null || raw.isBlank()) {
            return new ScoreParts(null, null, null, null);
        }
        String text = raw.replace('\u00a0', ' ').trim();
        if (text.contains("--")) {
            return new ScoreParts(null, null, null, null);
        }
        Matcher extraPen = EXTRA_PEN.matcher(text);
        if (extraPen.find()) {
            return new ScoreParts(null, null, extraPen.group(1), extraPen.group(2));
        }
        Matcher ft = SCORE_PAIR.matcher(text);
        String fullTime = ft.find() ? ft.group(1) + ":" + ft.group(2) : null;
        String firstHalf = ft.find() ? ft.group(1) + ":" + ft.group(2) : null;
        return new ScoreParts(fullTime, firstHalf, null, null);
    }

    private static LocalTime parseKickoffTime(String timeText) {
        if (timeText == null || timeText.isBlank()) {
            return null;
        }
        try {
            return LocalTime.parse(timeText.trim(), KICKOFF_TIME);
        } catch (Exception e) {
            return null;
        }
    }

    private static String textOrNull(Element element) {
        if (element == null) {
            return null;
        }
        String text = element.text().trim();
        return text.isBlank() ? null : text;
    }

    record ScoreParts(String fullTime, String firstHalf, String extraTime, String penalty) {
    }
}
