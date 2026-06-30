package net.friendly_bets.twentyfourscore;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class TwentyFourScoreMatchPageParser {

    private static final Pattern MATCH_PATH = Pattern.compile("/football/match/(\\d+)-");
    private static final DateTimeFormatter KICKOFF = DateTimeFormatter.ofPattern("d MMMM yyyy HH:mm", new Locale("ru"));

    public TwentyFourScoreMatchDetails parse(String html, String matchPath) {
        if (html == null || html.isBlank()) {
            return null;
        }
        Document doc = Jsoup.parse(html);
        Element homeEl = doc.selectFirst("div.team.tm1 a");
        Element awayEl = doc.selectFirst("div.team.tm2 a");
        if (homeEl == null || awayEl == null) {
            return null;
        }
        long matchId = parseMatchId(matchPath);
        String fullTimeRaw = textOrNull(doc.selectFirst("#score"));
        String firstHalfRaw = textOrNull(doc.selectFirst("#times"));
        String statusText = textOrNull(doc.selectFirst("#status"));
        Element datetimeEl = doc.selectFirst("div.datetime");
        LocalDateTime kickoffAt = parseKickoff(datetimeEl != null ? datetimeEl.text() : null);
        Integer matchday = parseMatchday(datetimeEl != null ? datetimeEl.text() : null);
        TwentyFourScoreScheduleParser.ScoreParts fromScore =
                TwentyFourScoreScheduleParser.parseScoreText(fullTimeRaw);
        TwentyFourScoreScheduleParser.ScoreParts fromTimes =
                TwentyFourScoreScheduleParser.parseScoreText(firstHalfRaw);
        String ft = coalesce(fromScore.fullTime(), normalizeScore(fullTimeRaw));
        String fh = coalesce(fromScore.firstHalf(), fromTimes.firstHalf(), fromTimes.fullTime());
        String ot = coalesce(fromScore.extraTime(), fromTimes.extraTime());
        String pen = coalesce(fromScore.penalty(), fromTimes.penalty());
        return TwentyFourScoreMatchDetails.builder()
                .externalMatchId(matchId)
                .matchPath(matchPath)
                .homeTeamName(homeEl.text().trim())
                .awayTeamName(awayEl.text().trim())
                .statusText(statusText)
                .fullTimeScore(ft)
                .firstHalfScore(fh)
                .extraTimeScore(ot)
                .penaltyScore(pen)
                .liveMinuteLabel(TwentyFourScoreStatusMapper.extractLiveMinute(statusText))
                .matchday(matchday)
                .kickoffAt(kickoffAt)
                .build();
    }

    private static long parseMatchId(String matchPath) {
        if (matchPath == null) {
            return 0L;
        }
        Matcher matcher = MATCH_PATH.matcher(matchPath);
        if (matcher.find()) {
            return Long.parseLong(matcher.group(1));
        }
        return 0L;
    }

    private static LocalDateTime parseKickoff(String text) {
        if (text == null || text.isBlank()) {
            return null;
        }
        String cleaned = text.replaceAll("Тур\\s+\\d+", "").trim();
        try {
            return LocalDateTime.parse(cleaned, KICKOFF);
        } catch (Exception e) {
            return null;
        }
    }

    private static Integer parseMatchday(String text) {
        if (text == null) {
            return null;
        }
        Matcher matcher = Pattern.compile("Тур\\s+(\\d+)").matcher(text);
        if (matcher.find()) {
            return Integer.parseInt(matcher.group(1));
        }
        return null;
    }

    private static String normalizeScore(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        return raw.trim().replaceAll("\\s+", "");
    }

    private static String coalesce(String... values) {
        if (values == null) {
            return null;
        }
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value.trim();
            }
        }
        return null;
    }

    private static String textOrNull(Element element) {
        if (element == null) {
            return null;
        }
        String text = element.text().trim();
        return text.isBlank() ? null : text;
    }
}
