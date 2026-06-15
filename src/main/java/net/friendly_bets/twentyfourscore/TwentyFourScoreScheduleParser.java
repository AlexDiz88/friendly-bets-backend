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
import java.util.EnumSet;
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
    private static final Pattern LIVE_MINUTE_SUFFIX = Pattern.compile(
            "\\s(\\d+(?:\\+\\d+)?)\\s*['′]\\s*$"
    );
    private static final DateTimeFormatter KICKOFF_TIME = DateTimeFormatter.ofPattern("HH:mm");

    public List<TwentyFourScoreListMatch> parseDailyPage(String html, LocalDate date, String competitionMarker) {
        return parseDailyPage(html, date, competitionMarker, null);
    }

    public List<TwentyFourScoreListMatch> parseDailyPagePreview(String html, LocalDate date) {
        List<TwentyFourScoreListMatch> matches = new ArrayList<>();
        for (TwentyFourScoreLeagueSection section : EnumSet.of(
                TwentyFourScoreLeagueSection.WORLD_CUP,
                TwentyFourScoreLeagueSection.FRIENDLIES
        )) {
            String marker = competitionMarker(section);
            if (marker == null) {
                continue;
            }
            matches.addAll(parseDailyPage(html, date, marker, section));
        }
        return matches;
    }

    private static String competitionMarker(TwentyFourScoreLeagueSection section) {
        return switch (section) {
            case WORLD_CUP -> TwentyFourScoreCompetitionMapping.worldCupPathMarker();
            case FRIENDLIES -> TwentyFourScoreCompetitionMapping.friendliesPathMarker();
        };
    }

    private List<TwentyFourScoreListMatch> parseDailyPage(
            String html,
            LocalDate date,
            String competitionMarker,
            TwentyFourScoreLeagueSection section
    ) {
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
                    matches.add(section != null ? parsed.toBuilder().section(section.name()).build() : parsed);
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
                .statusText(scores.statusText())
                .liveMinuteLabel(scores.liveMinuteLabel())
                .build();
    }

    static ScoreParts parseScoreText(String raw) {
        if (raw == null || raw.isBlank()) {
            return new ScoreParts(null, null, null, null, null, null);
        }
        String text = raw.replace('\u00a0', ' ').trim();
        if (text.contains("--")) {
            return new ScoreParts(null, null, null, null, null, null);
        }
        String liveMinuteLabel = null;
        Matcher minuteMatcher = LIVE_MINUTE_SUFFIX.matcher(text);
        if (minuteMatcher.find()) {
            liveMinuteLabel = minuteMatcher.group(1) + "'";
            text = text.substring(0, minuteMatcher.start()).trim();
        }
        Matcher extraPen = EXTRA_PEN.matcher(text);
        if (extraPen.find()) {
            return new ScoreParts(null, null, extraPen.group(1), extraPen.group(2), liveMinuteLabel, liveStatusText(liveMinuteLabel));
        }
        Matcher ft = SCORE_PAIR.matcher(text);
        String fullTime = ft.find() ? ft.group(1) + ":" + ft.group(2) : null;
        String firstHalf = ft.find() ? ft.group(1) + ":" + ft.group(2) : null;
        String statusText = liveMinuteLabel != null
                ? liveStatusText(liveMinuteLabel)
                : (fullTime != null ? "Завершен" : null);
        return new ScoreParts(fullTime, firstHalf, null, null, liveMinuteLabel, statusText);
    }

    private static String liveStatusText(String liveMinuteLabel) {
        return liveMinuteLabel != null ? "Идёт " + liveMinuteLabel : null;
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

    record ScoreParts(
            String fullTime,
            String firstHalf,
            String extraTime,
            String penalty,
            String liveMinuteLabel,
            String statusText
    ) {
    }
}
