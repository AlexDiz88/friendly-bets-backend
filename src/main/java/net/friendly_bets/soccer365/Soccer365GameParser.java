package net.friendly_bets.soccer365;

import net.friendly_bets.gameresults.GameScoreFromGoals;
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

/**
 * Parses soccer365.ru game card HTML ({@code /games/{id}/}).
 */
@Component
public class Soccer365GameParser {

    private static final Pattern MINUTE = Pattern.compile("(\\d{1,3})");

    public Soccer365ParsedFullMatch parse(String html) {
        Document doc = Jsoup.parse(html != null ? html : "");
        String statusText = normalizeStatusText(textOrEmpty(doc.selectFirst(".live_game_status")));
        List<MatchGoalEvent> goals = parseGoals(doc);
        GameScore score = GameScoreFromGoals.from(goals);
        MatchTeamStats stats = parseFullMatchStats(doc);
        return Soccer365ParsedFullMatch.builder()
                .statusText(statusText)
                .homeTeamName(textOrEmpty(doc.selectFirst(".live_game_ht a")))
                .awayTeamName(textOrEmpty(doc.selectFirst(".live_game_at a")))
                .competitionName(parseCompetitionName(doc))
                .gameScore(score)
                .goals(goals)
                .stats(stats)
                .build();
    }

    private static String parseCompetitionName(Document doc) {
        String fromHeader = textOrEmpty(doc.selectFirst("#game_events .block_title a, .block_header .block_title a"));
        if (!fromHeader.isBlank()) {
            return fromHeader;
        }
        return blankToNull(textOrEmpty(doc.selectFirst("a[href*=/competitions/]")));
    }

    private List<MatchGoalEvent> parseGoals(Document doc) {
        List<MatchGoalEvent> goals = new ArrayList<>();
        Elements rows = doc.select(".block_body.game_events > div");
        for (Element row : rows) {
            Element minEl = row.selectFirst(".event_min");
            if (minEl == null) {
                continue;
            }
            Element homeIcon = firstIcon(row, true);
            Element awayIcon = firstIcon(row, false);
            if (homeIcon == null && awayIcon == null) {
                continue;
            }
            Element icon = homeIcon != null ? homeIcon : awayIcon;
            boolean onHomeSide = homeIcon != null;
            boolean ownGoal = icon.hasClass("live_owngoal");
            boolean penalty = icon.hasClass("live_pengoal") || icon.hasClass("live_mispen");
            boolean shootoutGoal = icon.hasClass("live_psg");
            boolean shootoutMiss = icon.hasClass("live_psm");
            boolean inMatchMiss = icon.hasClass("live_mispen");
            boolean penaltyShootout = shootoutGoal || shootoutMiss;
            boolean missed = shootoutMiss || inMatchMiss;
            // soccer365 places the goal icon on the side that is credited on the scoreboard
            // (for own goals: benefiting team, not the player who scored into own net).
            String side = onHomeSide ? "HOME" : "AWAY";
            Element sideBlock = onHomeSide ? row.selectFirst(".event_ht") : row.selectFirst(".event_at");
            String player = sideBlock != null ? textOrEmpty(sideBlock.selectFirst(".img16 span a, .img16 span")) : null;
            if (player == null || player.isBlank()) {
                player = sideBlock != null ? textOrEmpty(sideBlock) : null;
                if (player != null) {
                    // strip icon-only noise
                    player = player.replaceAll("\\s+", " ").trim();
                }
            }
            String minuteLabel = normalizeMinuteLabel(minEl);
            goals.add(MatchGoalEvent.builder()
                    .minute(minuteLabel)
                    .minuteNumber(parseMinuteNumber(minuteLabel))
                    .teamSide(side)
                    .playerName(blankToNull(player))
                    .penalty(penalty)
                    .penaltyShootout(penaltyShootout)
                    .ownGoal(ownGoal)
                    .missed(missed)
                    .build());
        }
        return goals;
    }

    private static Element firstIcon(Element row, boolean home) {
        String prefix = home ? ".event_ht_icon" : ".event_at_icon";
        Element el = row.selectFirst(prefix + ".live_goal");
        if (el != null) {
            return el;
        }
        el = row.selectFirst(prefix + ".live_pengoal");
        if (el != null) {
            return el;
        }
        el = row.selectFirst(prefix + ".live_owngoal");
        if (el != null) {
            return el;
        }
        el = row.selectFirst(prefix + ".live_psg");
        if (el != null) {
            return el;
        }
        el = row.selectFirst(prefix + ".live_psm");
        if (el != null) {
            return el;
        }
        return row.selectFirst(prefix + ".live_mispen");
    }

    /**
     * Keep only the finished marker; drop score tails like {@code "Завершен. 7:6 по пенальти"}.
     * DB status is written as {@code FINISHED} by FULL_MATCH separately.
     */
    static String normalizeStatusText(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        String trimmed = raw.replace('\u00a0', ' ').trim();
        String lower = trimmed.toLowerCase(Locale.ROOT);
        if (lower.startsWith("завершен")) {
            return "Завершен";
        }
        if (lower.startsWith("finished")) {
            return "Finished";
        }
        return trimmed;
    }

    private MatchTeamStats parseFullMatchStats(Document doc) {
        Element full = doc.selectFirst("#stat-tp0");
        if (full == null) {
            full = doc.selectFirst("#stats #clubs_stats");
        }
        if (full == null) {
            return null;
        }
        Integer shotsH = null, shotsA = null;
        Integer sotH = null, sotA = null;
        Integer possH = null, possA = null;
        Integer ycH = null, ycA = null;
        Double xgH = null, xgA = null;
        for (Element item : full.select(".stats_item")) {
            String title = textOrEmpty(item.selectFirst(".stats_title")).toLowerCase(Locale.ROOT);
            Elements vals = item.select(".stats_inf");
            if (vals.size() < 2) {
                continue;
            }
            if (title.contains("xg") || title.contains("ожидаемые голы")) {
                xgH = parseDoubleSafe(vals.get(0).text());
                xgA = parseDoubleSafe(vals.get(1).text());
                continue;
            }
            Integer home = parseIntSafe(vals.get(0).text());
            Integer away = parseIntSafe(vals.get(1).text());
            if (title.contains("удары в створ")) {
                sotH = home;
                sotA = away;
            } else if (title.equals("удары")) {
                // exact title only — not "Удары в каркас" / "Удары в створ"
                shotsH = home;
                shotsA = away;
            } else if (title.contains("владение")) {
                possH = home;
                possA = away;
            } else if (title.contains("желт")) {
                ycH = home;
                ycA = away;
            }
        }
        if (shotsH == null && sotH == null && possH == null && ycH == null && xgH == null) {
            return null;
        }
        return MatchTeamStats.builder()
                .shotsHome(shotsH)
                .shotsAway(shotsA)
                .shotsOnTargetHome(sotH)
                .shotsOnTargetAway(sotA)
                .possessionHome(possH)
                .possessionAway(possA)
                .yellowCardsHome(ycH)
                .yellowCardsAway(ycA)
                .xgHome(xgH)
                .xgAway(xgA)
                .build();
    }

    private static String normalizeMinuteLabel(Element minEl) {
        String raw = minEl.text().replace('\u00a0', ' ').trim().replace("'", "");
        // Keep full stoppage labels like "45+3" / "90+6" when present in text.
        if (raw.contains("+")) {
            return raw;
        }
        if (minEl.selectFirst("sup") != null) {
            Matcher m = MINUTE.matcher(raw);
            if (m.find()) {
                return m.group(1) + "+";
            }
        }
        return raw;
    }

    private static Integer parseMinuteNumber(String label) {
        if (label == null) {
            return null;
        }
        Matcher m = MINUTE.matcher(label);
        if (!m.find()) {
            return null;
        }
        try {
            return Integer.parseInt(m.group(1));
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private static Integer parseIntSafe(String text) {
        if (text == null) {
            return null;
        }
        Matcher m = Pattern.compile("-?\\d+").matcher(text.trim());
        if (!m.find()) {
            return null;
        }
        try {
            return Integer.parseInt(m.group());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private static Double parseDoubleSafe(String text) {
        if (text == null) {
            return null;
        }
        String normalized = text.trim().replace(',', '.');
        Matcher m = Pattern.compile("-?\\d+(?:\\.\\d+)?").matcher(normalized);
        if (!m.find()) {
            return null;
        }
        try {
            return Double.parseDouble(m.group());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private static String textOrEmpty(Element el) {
        return el == null ? "" : el.text().replace('\u00a0', ' ').trim();
    }

    private static String blankToNull(String s) {
        return s == null || s.isBlank() ? null : s.trim();
    }
}
