package net.friendly_bets.soccer365;

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

    private static final Pattern SCORE = Pattern.compile("(\\d{1,2})\\s*:\\s*(\\d{1,2})");
    private static final Pattern MINUTE = Pattern.compile("(\\d{1,3})");

    public Soccer365ParsedFullMatch parse(String html) {
        Document doc = Jsoup.parse(html != null ? html : "");
        String statusText = textOrEmpty(doc.selectFirst(".live_game_status"));
        List<MatchGoalEvent> goals = parseGoals(doc);
        GameScore score = buildScore(statusText, goals, doc);
        MatchTeamStats stats = parseFullMatchStats(doc);
        return Soccer365ParsedFullMatch.builder()
                .statusText(statusText)
                .gameScore(score)
                .goals(goals)
                .stats(stats)
                .build();
    }

    private List<MatchGoalEvent> parseGoals(Document doc) {
        List<MatchGoalEvent> goals = new ArrayList<>();
        Elements rows = doc.select(".block_body.game_events > div");
        for (Element row : rows) {
            Element minEl = row.selectFirst(".event_min");
            if (minEl == null) {
                continue;
            }
            Element homeGoal = row.selectFirst(".event_ht_icon.live_goal, .event_ht_icon.live_psg");
            Element awayGoal = row.selectFirst(".event_at_icon.live_goal, .event_at_icon.live_psg");
            if (homeGoal == null && awayGoal == null) {
                continue;
            }
            boolean penaltyShootout = (homeGoal != null && homeGoal.hasClass("live_psg"))
                    || (awayGoal != null && awayGoal.hasClass("live_psg"));
            String side = homeGoal != null ? "HOME" : "AWAY";
            Element sideBlock = homeGoal != null ? row.selectFirst(".event_ht") : row.selectFirst(".event_at");
            String player = sideBlock != null ? textOrEmpty(sideBlock.selectFirst(".img16 span a, .img16 span")) : null;
            String minuteLabel = normalizeMinuteLabel(minEl);
            goals.add(MatchGoalEvent.builder()
                    .minute(minuteLabel)
                    .minuteNumber(parseMinuteNumber(minuteLabel))
                    .teamSide(side)
                    .playerName(blankToNull(player))
                    .penaltyShootout(penaltyShootout)
                    .build());
        }
        return goals;
    }

    private GameScore buildScore(String statusText, List<MatchGoalEvent> goals, Document doc) {
        int htHome = 0, htAway = 0;
        int ftHome = 0, ftAway = 0;
        int otHome = 0, otAway = 0;
        int penHome = 0, penAway = 0;
        boolean anyOt = false;
        boolean anyPen = false;

        for (MatchGoalEvent goal : goals) {
            if (Boolean.TRUE.equals(goal.getPenaltyShootout())) {
                anyPen = true;
                if ("HOME".equals(goal.getTeamSide())) {
                    penHome++;
                } else {
                    penAway++;
                }
                continue;
            }
            int minute = goal.getMinuteNumber() != null ? goal.getMinuteNumber() : 0;
            if ("HOME".equals(goal.getTeamSide())) {
                if (minute <= 45) {
                    htHome++;
                }
                if (minute <= 90) {
                    ftHome++;
                } else {
                    anyOt = true;
                    otHome++;
                }
            } else {
                if (minute <= 45) {
                    htAway++;
                }
                if (minute <= 90) {
                    ftAway++;
                } else {
                    anyOt = true;
                    otAway++;
                }
            }
        }

        int afterOtHome = ftHome + otHome;
        int afterOtAway = ftAway + otAway;

        // Prefer board score (after OT, before pens) when present.
        Element board = doc.selectFirst(".live_game_goals");
        if (board != null) {
            Elements spans = board.select(".live_game_goal span");
            if (spans.size() >= 2) {
                Integer h = parseIntSafe(spans.get(0).text());
                Integer a = parseIntSafe(spans.get(1).text());
                if (h != null && a != null) {
                    afterOtHome = h;
                    afterOtAway = a;
                    if (afterOtHome != ftHome || afterOtAway != ftAway) {
                        anyOt = true;
                    }
                }
            }
        }

        Matcher penMatcher = SCORE.matcher(statusText != null ? statusText : "");
        if (statusText != null && statusText.toLowerCase(Locale.ROOT).contains("пенал") && penMatcher.find()) {
            anyPen = true;
            penHome = Integer.parseInt(penMatcher.group(1));
            penAway = Integer.parseInt(penMatcher.group(2));
        }

        GameScore.GameScoreBuilder builder = GameScore.builder()
                .fullTime(formatScore(ftHome, ftAway))
                .firstTime(formatScore(htHome, htAway));
        if (anyOt) {
            builder.overTime(formatScore(afterOtHome, afterOtAway));
        }
        if (anyPen) {
            builder.penalty(formatScore(penHome, penAway));
            if (!anyOt && (afterOtHome != ftHome || afterOtAway != ftAway)) {
                builder.overTime(formatScore(afterOtHome, afterOtAway));
            }
        }
        return builder.build();
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
        for (Element item : full.select(".stats_item")) {
            String title = textOrEmpty(item.selectFirst(".stats_title")).toLowerCase(Locale.ROOT);
            Elements vals = item.select(".stats_inf");
            if (vals.size() < 2) {
                continue;
            }
            Integer home = parseIntSafe(vals.get(0).text());
            Integer away = parseIntSafe(vals.get(1).text());
            if (title.contains("удары в створ")) {
                sotH = home;
                sotA = away;
            } else if (title.equals("удары") || title.startsWith("удары ")) {
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
        if (shotsH == null && sotH == null && possH == null && ycH == null) {
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
                .build();
    }

    private static String normalizeMinuteLabel(Element minEl) {
        String raw = minEl.text().replace('\u00a0', ' ').trim().replace("'", "");
        if (minEl.selectFirst("sup") != null && !raw.contains("+")) {
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
            int base = Integer.parseInt(m.group(1));
            if (label.contains("+")) {
                // injury time: keep base minute for bucketing (90+/120+)
                return base;
            }
            return base;
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

    private static String formatScore(int home, int away) {
        return home + ":" + away;
    }

    private static String textOrEmpty(Element el) {
        return el == null ? "" : el.text().replace('\u00a0', ' ').trim();
    }

    private static String blankToNull(String s) {
        return s == null || s.isBlank() ? null : s.trim();
    }
}
