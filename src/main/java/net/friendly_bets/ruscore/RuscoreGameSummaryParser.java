package net.friendly_bets.ruscore;

import net.friendly_bets.exceptions.BadRequestException;
import net.friendly_bets.matchschedule.GameScoreFromGoals;
import net.friendly_bets.models.GameScore;
import net.friendly_bets.models.schedule.MatchGoalEvent;
import net.friendly_bets.models.schedule.MatchTeamStats;
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

@Component
public class RuscoreGameSummaryParser {

    private static final Pattern ADDED_TIME = Pattern.compile(
            "добавлено\\s*\\+\\s*(\\d+)",
            Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE
    );
    private static final Pattern MINUTE_NUMBER = Pattern.compile("(\\d{1,3})");
    /** Score box on a real goal incident, e.g. "1 : 0" / "3:0". */
    private static final Pattern SCORE_BOX = Pattern.compile("(\\d{1,2})\\s*:\\s*(\\d{1,2})");
    private static final Pattern PERIOD_TITLE = Pattern.compile(
            "([12])-й\\s+тайм\\s*(\\d{1,2})\\s*:\\s*(\\d{1,2})",
            Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE
    );

    public RuscoreParsedFullMatch parse(String html, String eventId, String slug) {
        if (html == null || html.isBlank()) {
            throw new BadRequestException("ruscoreParseFailed");
        }
        Document doc = Jsoup.parse(html);
        String statusText = text(doc.selectFirst("[data-test-id=status]"));
        String overall = text(doc.selectFirst("[data-test-id=overall]"));
        String homeTeam = firstTeamName(doc, true);
        String awayTeam = firstTeamName(doc, false);
        String competition = competitionName(doc);

        List<MatchGoalEvent> goals = parseGoals(doc);
        MatchTeamStats stats = parseStats(doc);
        Integer[] added = parseAddedTimes(doc);
        String firstTimeFromPeriod = parseFirstHalfScore(doc);

        GameScore fromGoals = GameScoreFromGoals.from(goals);
        GameScore board = scoreFromOverall(overall);
        String fullTime = board != null ? board.getFullTime()
                : (fromGoals != null ? fromGoals.getFullTime() : null);
        String firstTime = firstTimeFromPeriod;
        if (firstTime == null && fromGoals != null) {
            firstTime = fromGoals.getFirstTime();
        }
        GameScore gameScore = fullTime == null && firstTime == null
                ? null
                : GameScore.builder()
                .fullTime(fullTime)
                .firstTime(firstTime)
                .overTime(fromGoals != null ? fromGoals.getOverTime() : null)
                .penalty(fromGoals != null ? fromGoals.getPenalty() : null)
                .build();

        return RuscoreParsedFullMatch.builder()
                .eventId(eventId)
                .slug(slug)
                .statusText(statusText)
                .homeTeamName(homeTeam)
                .awayTeamName(awayTeam)
                .competitionName(competition)
                .gameScore(gameScore)
                .goals(goals)
                .stats(stats)
                .addedTimeFirstHalf(added[0])
                .addedTimeSecondHalf(added[1])
                .build();
    }

    private static List<MatchGoalEvent> parseGoals(Document doc) {
        List<MatchGoalEvent> goals = new ArrayList<>();
        Elements incidents = doc.select("[data-test-id=incident-wrapper]");
        for (Element inc : incidents) {
            if (isSubstitution(inc)) {
                continue;
            }
            CardKind card = detectCard(inc);
            if (card == CardKind.YELLOW) {
                continue;
            }
            if (card == CardKind.RED || card == CardKind.SECOND_YELLOW) {
                MatchGoalEvent red = parseCardEvent(inc, card);
                if (red != null) {
                    goals.add(red);
                }
                continue;
            }
            if (!hasGoalScoreBox(inc)) {
                // VAR ("Гол засчитан"), missed penalty, etc. — no score box → not a goal
                continue;
            }
            String timeRaw = text(inc.selectFirst("[data-test-id=time]"));
            if (timeRaw == null) {
                continue;
            }
            Elements names = inc.select("[data-test-id=player-name]");
            if (names.isEmpty()) {
                continue;
            }
            String player = text(names.first());
            if (player == null || isNonGoalPlayerLabel(player)) {
                continue;
            }
            String side = teamSide(inc);
            if (side == null) {
                continue;
            }
            String minute = normalizeMinuteLabel(timeRaw);
            Boolean penalty = isInMatchPenaltyGoal(inc);
            goals.add(MatchGoalEvent.builder()
                    .minute(minute)
                    .minuteNumber(parseMinuteNumber(minute))
                    .teamSide(side)
                    .playerName(player)
                    .penalty(penalty)
                    .build());
        }
        return goals;
    }

    private static MatchGoalEvent parseCardEvent(Element inc, CardKind card) {
        String timeRaw = text(inc.selectFirst("[data-test-id=time]"));
        if (timeRaw == null) {
            return null;
        }
        Elements names = inc.select("[data-test-id=player-name]");
        if (names.isEmpty()) {
            return null;
        }
        String player = text(names.first());
        if (player == null) {
            return null;
        }
        String side = teamSide(inc);
        if (side == null) {
            return null;
        }
        String minute = normalizeMinuteLabel(timeRaw);
        return MatchGoalEvent.builder()
                .minute(minute)
                .minuteNumber(parseMinuteNumber(minute))
                .teamSide(side)
                .playerName(player)
                .redCard(true)
                .secondYellow(card == CardKind.SECOND_YELLOW ? true : null)
                .build();
    }

    private static String teamSide(Element inc) {
        if (inc.selectFirst("[data-test-id=home-player]") != null) {
            return "HOME";
        }
        if (inc.selectFirst("[data-test-id=away-player]") != null) {
            return "AWAY";
        }
        return null;
    }

    /**
     * Real goals render a running score box (e.g. "1 : 0") inside the incident; VAR / missed pen do not.
     */
    private static boolean hasGoalScoreBox(Element inc) {
        String raw = inc.text();
        if (raw == null || raw.isBlank()) {
            return false;
        }
        return SCORE_BOX.matcher(raw.replace('\u00a0', ' ')).find();
    }

    /**
     * In-match penalty goal: score box subtype {@code 11} (UI shows "11-м" via CSS).
     * Shootout / missed pens are not marked this way on scored goals.
     */
    private static Boolean isInMatchPenaltyGoal(Element inc) {
        for (Element el : inc.select("span[class*=scoreSubtype]")) {
            String t = text(el);
            if (t == null) {
                continue;
            }
            String norm = t.toLowerCase(Locale.ROOT)
                    .replace('ё', 'е')
                    .replace('\u00a0', ' ')
                    .replace(" ", "");
            if ("11".equals(norm)
                    || norm.startsWith("11-")
                    || "11м".equals(norm)
                    || norm.contains("пеналт")) {
                return true;
            }
        }
        return null;
    }

    private static boolean isNonGoalPlayerLabel(String player) {
        String lower = player.toLowerCase(Locale.ROOT).replace('ё', 'е');
        return lower.contains("гол засчитан")
                || lower.contains("гол отменен")
                || lower.contains("отмененный гол")
                || lower.contains("гол не засчитан");
    }

    private static boolean isSubstitution(Element inc) {
        for (Element svg : inc.select("svg")) {
            String cls = svg.className();
            if (cls != null && (cls.contains("exchange") || cls.contains("_exchange_"))) {
                return true;
            }
        }
        return false;
    }

    /**
     * Card icon SVG: yellow-only, straight red, or second yellow (yellow+red stacked).
     */
    private static CardKind detectCard(Element inc) {
        Element svg = null;
        for (Element candidate : inc.select("svg")) {
            String cls = candidate.className();
            if (cls != null && cls.contains("iconCard")) {
                svg = candidate;
                break;
            }
        }
        if (svg == null) {
            return null;
        }
        String inner = svg.html();
        if (inner == null) {
            inner = "";
        }
        boolean yellow = containsAnyIgnoreCase(inner, "#FFD600", "#EBC707");
        boolean red = containsAnyIgnoreCase(inner, "#FF5050", "#FC3636", "#EB4747");
        if (yellow && red) {
            return CardKind.SECOND_YELLOW;
        }
        if (red) {
            return CardKind.RED;
        }
        if (yellow) {
            return CardKind.YELLOW;
        }
        // Unknown card glyph — treat as non-goal card, skip.
        return CardKind.YELLOW;
    }

    private static boolean containsAnyIgnoreCase(String haystack, String... needles) {
        String lower = haystack.toLowerCase(Locale.ROOT);
        for (String n : needles) {
            if (lower.contains(n.toLowerCase(Locale.ROOT))) {
                return true;
            }
        }
        return false;
    }

    private enum CardKind {
        YELLOW,
        RED,
        SECOND_YELLOW
    }

    private static MatchTeamStats parseStats(Document doc) {
        Integer possessionHome = null, possessionAway = null;
        Integer shotsHome = null, shotsAway = null;
        Integer sotHome = null, sotAway = null;
        Integer cornersHome = null, cornersAway = null;
        Integer offsidesHome = null, offsidesAway = null;
        Integer savesHome = null, savesAway = null;
        Integer yellowHome = null, yellowAway = null;
        Integer redHome = null, redAway = null;
        Integer secondYellowHome = null, secondYellowAway = null;

        for (Element item : doc.select("[data-test-id=stat-item]")) {
            String metric = text(item.selectFirst("[data-test-id=metric-name]"));
            if (metric == null) {
                continue;
            }
            Integer home = parseInt(text(item.selectFirst("[data-test-id=home-score]")));
            Integer away = parseInt(text(item.selectFirst("[data-test-id=away-score]")));
            String key = metric.toLowerCase(Locale.ROOT).replace('ё', 'е');
            if (key.contains("владение")) {
                possessionHome = home;
                possessionAway = away;
            } else if (key.contains("всего ударов")) {
                shotsHome = home;
                shotsAway = away;
            } else if (key.contains("удары в створ") || key.equals("в створ")) {
                sotHome = home;
                sotAway = away;
            } else if (key.equals("угловые") || key.startsWith("угловые ")) {
                cornersHome = home;
                cornersAway = away;
            } else if (key.contains("офсайд")) {
                offsidesHome = home;
                offsidesAway = away;
            } else if (key.contains("сэйв") || key.contains("сейв")) {
                savesHome = home;
                savesAway = away;
            } else if (key.contains("желтые") && key.contains("карточки") && key.contains("втор")) {
                // "Вторые жёлтые карточки" → count as reds
                secondYellowHome = home;
                secondYellowAway = away;
            } else if (key.contains("желтые") && key.contains("карточки")) {
                yellowHome = home;
                yellowAway = away;
            } else if (key.contains("красные") && key.contains("карточки") && !key.contains("втор")) {
                redHome = home;
                redAway = away;
            }
        }

        redHome = sumNullable(redHome, secondYellowHome);
        redAway = sumNullable(redAway, secondYellowAway);

        if (possessionHome == null && shotsHome == null && sotHome == null
                && cornersHome == null && offsidesHome == null && savesHome == null
                && yellowHome == null && redHome == null) {
            return null;
        }
        return MatchTeamStats.builder()
                .possessionHome(possessionHome)
                .possessionAway(possessionAway)
                .shotsHome(shotsHome)
                .shotsAway(shotsAway)
                .shotsOnTargetHome(sotHome)
                .shotsOnTargetAway(sotAway)
                .cornersHome(cornersHome)
                .cornersAway(cornersAway)
                .offsidesHome(offsidesHome)
                .offsidesAway(offsidesAway)
                .savesHome(savesHome)
                .savesAway(savesAway)
                .yellowCardsHome(yellowHome)
                .yellowCardsAway(yellowAway)
                .redCardsHome(redHome)
                .redCardsAway(redAway)
                .build();
    }

    private static Integer sumNullable(Integer a, Integer b) {
        if (a == null && b == null) {
            return null;
        }
        return (a != null ? a : 0) + (b != null ? b : 0);
    }

    /**
     * One added-time value per half from period accordion sections (avoids duplicate HT labels).
     *
     * @return [firstHalf, secondHalf] minutes; either may be null
     */
    private static Integer[] parseAddedTimes(Document doc) {
        Integer first = null;
        Integer second = null;
        for (Element header : doc.select("[data-test-id=accordion-header]")) {
            String headerText = text(header);
            if (headerText == null) {
                continue;
            }
            String lower = headerText.toLowerCase(Locale.ROOT).replace('ё', 'е');
            boolean firstHalf = lower.contains("1-й тайм");
            boolean secondHalf = lower.contains("2-й тайм");
            if (!firstHalf && !secondHalf) {
                continue;
            }
            Element content = header.nextElementSibling();
            if (content == null || !"accordion-content-open".equals(content.attr("data-test-id"))) {
                // search sibling with content open
                Element sib = header.nextElementSibling();
                while (sib != null && !"accordion-content-open".equals(sib.attr("data-test-id"))) {
                    sib = sib.nextElementSibling();
                }
                content = sib;
            }
            if (content == null) {
                continue;
            }
            Integer value = firstAddedTimeIn(content);
            if (value == null) {
                continue;
            }
            if (firstHalf && first == null) {
                first = value;
            } else if (secondHalf && second == null) {
                second = value;
            }
        }
        if (first == null && second == null) {
            // fallback: first + last distinct labels in document order
            List<Integer> values = new ArrayList<>();
            for (Element el : doc.select("span")) {
                String cls = el.className();
                if (cls == null || !cls.contains("addedTimeLabelText")) {
                    continue;
                }
                Integer v = parseAddedLabel(text(el));
                if (v != null) {
                    values.add(v);
                }
            }
            if (!values.isEmpty()) {
                first = values.get(0);
                if (values.size() >= 2) {
                    second = values.get(values.size() - 1);
                }
            }
        }
        return new Integer[]{first, second};
    }

    private static Integer firstAddedTimeIn(Element root) {
        for (Element el : root.select("span")) {
            String cls = el.className();
            if (cls == null || !cls.contains("addedTimeLabelText")) {
                continue;
            }
            Integer v = parseAddedLabel(text(el));
            if (v != null) {
                return v;
            }
        }
        return null;
    }

    private static Integer parseAddedLabel(String t) {
        if (t == null) {
            return null;
        }
        Matcher m = ADDED_TIME.matcher(t.toLowerCase(Locale.ROOT).replace('ё', 'е'));
        if (!m.find()) {
            return null;
        }
        return Integer.parseInt(m.group(1));
    }

    private static String parseFirstHalfScore(Document doc) {
        for (Element el : doc.select("[data-test-id=title], [data-test-id=accordion-header]")) {
            String t = text(el);
            if (t == null) {
                continue;
            }
            Matcher m = PERIOD_TITLE.matcher(t.toLowerCase(Locale.ROOT).replace('ё', 'е'));
            if (m.find() && "1".equals(m.group(1))) {
                return m.group(2) + ":" + m.group(3);
            }
        }
        return null;
    }

    private static String competitionName(Document doc) {
        // Prefer tournament link near the match header teams, not sidebar "Премьер-Лига".
        Element teamInfo = doc.selectFirst("[data-test-id=team-info]");
        if (teamInfo != null) {
            Element root = teamInfo;
            for (int i = 0; i < 8 && root != null; i++) {
                Element link = root.selectFirst("a[href^=/tournament/]");
                if (link != null) {
                    String name = text(link);
                    if (name != null) {
                        return name;
                    }
                }
                root = root.parent();
            }
        }
        for (Element link : doc.select("a[href^=/tournament/]")) {
            String href = link.attr("href");
            if (href == null || href.contains("/standings") || href.contains("/calendar")) {
                continue;
            }
            String name = text(link);
            if (name != null && name.length() >= 3) {
                return name;
            }
        }
        return null;
    }

    private static String firstTeamName(Document doc, boolean home) {
        Elements infos = doc.select("[data-test-id=team-info]");
        if (infos.size() >= 2) {
            Element side = home ? infos.get(0) : infos.get(1);
            String name = text(side.selectFirst("[data-test-id=team-name]"));
            if (name != null) {
                return name;
            }
        }
        Elements names = doc.select("[data-test-id=team-name]");
        if (home && !names.isEmpty()) {
            return text(names.first());
        }
        if (!home && names.size() >= 2) {
            return text(names.get(1));
        }
        return null;
    }

    private static GameScore scoreFromOverall(String overall) {
        if (!looksLikeScore(overall)) {
            return null;
        }
        String normalized = overall.replace('\u00a0', ' ').trim().replace('：', ':');
        Matcher m = SCORE_BOX.matcher(normalized);
        if (!m.find()) {
            return null;
        }
        return GameScore.builder().fullTime(m.group(1) + ":" + m.group(2)).build();
    }

    private static boolean looksLikeScore(String raw) {
        if (raw == null) {
            return false;
        }
        return SCORE_BOX.matcher(raw.replace('\u00a0', ' ')).find();
    }

    private static String normalizeMinuteLabel(String raw) {
        String t = raw.replace('\u00a0', ' ')
                .replace('’', '\'')
                .replace('′', '\'')
                .trim();
        while (t.endsWith("'")) {
            t = t.substring(0, t.length() - 1).trim();
        }
        return t;
    }

    private static Integer parseMinuteNumber(String minute) {
        if (minute == null) {
            return null;
        }
        // Prefer base minute before '+': "45+6" → 45
        Matcher plus = Pattern.compile("(\\d{1,3})\\s*\\+").matcher(minute);
        if (plus.find()) {
            return Integer.parseInt(plus.group(1));
        }
        Matcher m = MINUTE_NUMBER.matcher(minute);
        if (!m.find()) {
            return null;
        }
        try {
            return Integer.parseInt(m.group(1));
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private static Integer parseInt(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        try {
            return Integer.parseInt(raw.trim().replace("%", ""));
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private static String text(Element el) {
        if (el == null) {
            return null;
        }
        String t = el.text();
        if (t == null) {
            return null;
        }
        t = t.replace('\u00a0', ' ').trim();
        return t.isEmpty() ? null : t;
    }
}
