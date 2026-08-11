package net.friendly_bets.flashscore;

import net.friendly_bets.exceptions.BadRequestException;
import net.friendly_bets.models.GameScore;
import net.friendly_bets.models.schedule.MatchGoalEvent;
import net.friendly_bets.models.schedule.MatchTeamStats;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class FlashscoreMatchDetailParser {

    private static final Pattern ADDED_MINUTE = Pattern.compile("\\+(\\d+)");
    private static final Pattern MINUTE_NUMBER = Pattern.compile("(\\d{1,3})");

    public FlashscoreParsedFullMatch parse(
            String summaryFeed,
            String statsFeed,
            String resultFeed,
            String eventId
    ) {
        return parse(summaryFeed, statsFeed, resultFeed, eventId, null);
    }

    public FlashscoreParsedFullMatch parse(
            String summaryFeed,
            String statsFeed,
            String resultFeed,
            String eventId,
            String h2hFeed
    ) {
        if (eventId == null || eventId.isBlank()) {
            throw new BadRequestException("flashscoreGameIdRequired");
        }
        Map<String, String> resultFields = parseAllRecordFields(resultFeed);
        String statusText = FlashscoreDayFeedParser.mapStatus(resultFields.get("AC"));
        if (statusText == null) {
            statusText = "finished";
        }

        ScoreBreakdown scores = parseScores(resultFields);
        List<MatchGoalEvent> goals = parseGoals(summaryFeed);
        Integer[] added = parseAddedTimes(summaryFeed);
        MatchTeamStats stats = parseStats(statsFeed);
        FlashscoreMatchMeta meta = parseMatchMeta(h2hFeed, eventId);

        GameScore gameScore = GameScore.builder()
                .fullTime(scores.fullTime())
                .firstTime(scores.firstTime())
                .build();

        return FlashscoreParsedFullMatch.builder()
                .eventId(eventId.trim())
                .statusText(statusText)
                .homeTeamName(meta != null ? meta.getHomeTeamName() : null)
                .awayTeamName(meta != null ? meta.getAwayTeamName() : null)
                .competitionName(meta != null ? meta.getCompetitionName() : null)
                .gameScore(gameScore)
                .goals(goals)
                .stats(stats)
                .addedTimeFirstHalf(added[0])
                .addedTimeSecondHalf(added[1])
                .build();
    }

    static FlashscoreMatchMeta parseMatchMeta(String h2hFeed, String eventId) {
        if (h2hFeed == null || h2hFeed.isBlank() || eventId == null || eventId.isBlank()) {
            return null;
        }
        String needle = eventId.trim();
        for (String record : FlashscoreFeedSupport.splitRecords(h2hFeed)) {
            Map<String, String> fields = FlashscoreFeedSupport.parseRecord(record);
            String kp = fields.get("KP");
            if (kp == null || !kp.equalsIgnoreCase(needle)) {
                continue;
            }
            String home = cleanTeamLabel(FlashscoreFeedSupport.firstNonBlank(fields, "KJ", "FH"));
            String away = cleanTeamLabel(FlashscoreFeedSupport.firstNonBlank(fields, "KK", "FK"));
            String competition = fields.get("KF");
            if (home == null && away == null && competition == null) {
                return null;
            }
            return FlashscoreMatchMeta.builder()
                    .homeTeamName(home)
                    .awayTeamName(away)
                    .competitionName(competition)
                    .build();
        }
        return null;
    }

    private static String cleanTeamLabel(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        String trimmed = raw.trim();
        if (trimmed.startsWith("*")) {
            trimmed = trimmed.substring(1).trim();
        }
        return trimmed.isEmpty() ? null : trimmed;
    }

    private record ScoreBreakdown(String fullTime, String firstTime) {
    }

    private static ScoreBreakdown parseScores(Map<String, String> resultFields) {
        if (resultFields == null || resultFields.isEmpty()) {
            return new ScoreBreakdown(null, null);
        }
        String htHome = resultFields.get("BC");
        String htAway = resultFields.get("BD");
        String shHome = resultFields.get("BA");
        String shAway = resultFields.get("BB");
        String firstTime = formatPair(htHome, htAway);
        String fullTime = null;
        if (htHome != null && htAway != null && shHome != null && shAway != null) {
            try {
                int ftH = Integer.parseInt(htHome.trim()) + Integer.parseInt(shHome.trim());
                int ftA = Integer.parseInt(htAway.trim()) + Integer.parseInt(shAway.trim());
                fullTime = ftH + ":" + ftA;
            } catch (NumberFormatException ignored) {
                fullTime = formatPair(shHome, shAway);
            }
        }
        if (fullTime == null) {
            fullTime = formatPair(resultFields.get("AG"), resultFields.get("AH"));
        }
        return new ScoreBreakdown(fullTime, firstTime);
    }

    private static String formatPair(String home, String away) {
        if (home == null || away == null || home.isBlank() || away.isBlank()) {
            return null;
        }
        return home.trim() + ":" + away.trim();
    }

    private static List<MatchGoalEvent> parseGoals(String summaryFeed) {
        List<MatchGoalEvent> goals = new ArrayList<>();
        if (summaryFeed == null || summaryFeed.isBlank()) {
            return goals;
        }
        for (String record : FlashscoreFeedSupport.splitRecords(summaryFeed)) {
            if (record.startsWith("IIIX÷")) {
                MatchGoalEvent varEvent = parseVarDisallowedBlock(record);
                if (varEvent != null) {
                    goals.add(varEvent);
                }
            } else if (record.startsWith("III÷")) {
                goals.addAll(parseIncidentBlock(record));
            }
        }
        return goals;
    }

    private static List<MatchGoalEvent> parseIncidentBlock(String block) {
        List<MatchGoalEvent> events = new ArrayList<>();
        String blockSide = null;
        String blockMinute = null;
        String subPlayer = null;
        boolean penaltyAwardedInBlock = false;
        for (String segment : block.split("¬")) {
            if (segment == null || segment.isBlank() || !segment.contains("÷")) {
                continue;
            }
            int idx = segment.indexOf('÷');
            String key = segment.substring(0, idx);
            String value = segment.substring(idx + 1);
            switch (key) {
                case "IA" -> blockSide = value;
                case "IB" -> blockMinute = value;
                case "IE" -> subPlayer = null;
                case "IF" -> subPlayer = value;
                case "IK" -> {
                    if (value == null) {
                        continue;
                    }
                    String kindLower = value.toLowerCase(Locale.ROOT);
                    if (kindLower.contains("substitution")) {
                        continue;
                    }
                    if (kindLower.contains("penalty awarded")) {
                        penaltyAwardedInBlock = true;
                        continue;
                    }
                    if (kindLower.contains("yellow")) {
                        continue;
                    }
                    if (kindLower.contains("red")) {
                        MatchGoalEvent card = parseCard(blockSide, blockMinute, subPlayer, kindLower);
                        if (card != null) {
                            events.add(card);
                        }
                        continue;
                    }
                    if (kindLower.contains("penalty missed")) {
                        MatchGoalEvent missed = parseMissedPenalty(blockSide, blockMinute, subPlayer);
                        if (missed != null) {
                            events.add(missed);
                        }
                        continue;
                    }
                    if (kindLower.contains("own goal")) {
                        MatchGoalEvent ownGoal = parseScoringEvent(
                                blockSide, blockMinute, subPlayer, true, false, false, false);
                        if (ownGoal != null) {
                            events.add(ownGoal);
                        }
                        continue;
                    }
                    if (kindLower.equals("penalty")) {
                        MatchGoalEvent penaltyGoal = parseScoringEvent(
                                blockSide, blockMinute, subPlayer, false, true, false, false);
                        if (penaltyGoal != null) {
                            events.add(penaltyGoal);
                        }
                        continue;
                    }
                    if (!kindLower.equals("goal") && !kindLower.startsWith("goal ")) {
                        continue;
                    }
                    boolean fromPenalty = penaltyAwardedInBlock;
                    MatchGoalEvent goal = parseScoringEvent(
                            blockSide, blockMinute, subPlayer, false, fromPenalty, false, false);
                    if (goal != null) {
                        events.add(goal);
                    }
                }
                default -> {
                }
            }
        }
        return events;
    }

    private static MatchGoalEvent parseVarDisallowedBlock(String block) {
        String blockSide = null;
        String blockMinute = null;
        String player = null;
        boolean disallowed = false;
        for (String segment : block.split("¬")) {
            if (segment == null || segment.isBlank() || !segment.contains("÷")) {
                continue;
            }
            int idx = segment.indexOf('÷');
            String key = segment.substring(0, idx);
            String value = segment.substring(idx + 1);
            switch (key) {
                case "IAX", "IA" -> blockSide = value;
                case "IBX", "IB" -> blockMinute = value;
                case "IFX", "IF" -> player = value;
                case "IKX", "IK" -> {
                    if (value != null && value.toLowerCase(Locale.ROOT).contains("goal disallowed")) {
                        disallowed = true;
                    }
                }
                default -> {
                }
            }
        }
        if (!disallowed || player == null || player.isBlank()) {
            return null;
        }
        String side = mapSide(blockSide);
        if (side == null) {
            return null;
        }
        return MatchGoalEvent.builder()
                .minute(normalizeMinute(blockMinute, null))
                .teamSide(side)
                .playerName(player.trim())
                .varDisallowed(true)
                .build();
    }

    private static MatchGoalEvent parseScoringEvent(
            String blockSide,
            String blockMinute,
            String player,
            boolean ownGoal,
            boolean penalty,
            boolean missed,
            boolean varDisallowed
    ) {
        if (player == null || player.isBlank()) {
            return null;
        }
        String side = mapSide(blockSide);
        if (side == null) {
            return null;
        }
        return MatchGoalEvent.builder()
                .minute(normalizeMinute(blockMinute, null))
                .teamSide(side)
                .playerName(player.trim())
                .ownGoal(ownGoal ? true : null)
                .penalty(penalty ? true : null)
                .missed(missed ? true : null)
                .varDisallowed(varDisallowed ? true : null)
                .build();
    }

    private static MatchGoalEvent parseMissedPenalty(String blockSide, String blockMinute, String player) {
        return parseScoringEvent(blockSide, blockMinute, player, false, true, true, false);
    }

    private static MatchGoalEvent parseCard(
            String blockSide,
            String blockMinute,
            String player,
            String kindLower
    ) {
        if (player == null || player.isBlank()) {
            return null;
        }
        String side = mapSide(blockSide);
        if (side == null) {
            return null;
        }
        boolean secondYellow = kindLower.contains("second");
        boolean red = kindLower.contains("red");
        return MatchGoalEvent.builder()
                .minute(normalizeMinute(blockMinute, null))
                .teamSide(side)
                .playerName(player.trim())
                .redCard(red || secondYellow)
                .secondYellow(secondYellow)
                .build();
    }

    private static String mapSide(String ia) {
        if ("1".equals(ia)) {
            return "HOME";
        }
        if ("2".equals(ia)) {
            return "AWAY";
        }
        return null;
    }

    private static String normalizeMinute(String raw, String halfLabel) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        String minute = raw.trim().replace("'", "");
        if (halfLabel != null) {
            String lower = halfLabel.toLowerCase(Locale.ROOT);
            if (lower.contains("1st") && minute.matches("\\d{1,2}")) {
                // keep as-is
            }
        }
        return minute;
    }

    private static Integer[] parseAddedTimes(String summaryFeed) {
        Integer first = null;
        Integer second = null;
        String currentHalf = null;
        for (String record : FlashscoreFeedSupport.splitRecords(summaryFeed)) {
            Map<String, String> fields = FlashscoreFeedSupport.parseRecord(record);
            String ac = fields.get("AC");
            if (ac != null && ac.toLowerCase(Locale.ROOT).contains("half")) {
                currentHalf = ac.toLowerCase(Locale.ROOT);
                continue;
            }
            String minute = fields.get("IB");
            if (minute == null || currentHalf == null) {
                continue;
            }
            Matcher m = ADDED_MINUTE.matcher(minute);
            if (!m.find()) {
                continue;
            }
            int added = Integer.parseInt(m.group(1));
            if (currentHalf.contains("1st") && first == null) {
                first = added;
            } else if (currentHalf.contains("2nd") && second == null) {
                second = added;
            }
        }
        return new Integer[]{first, second};
    }

    private static MatchTeamStats parseStats(String statsFeed) {
        if (statsFeed == null || statsFeed.isBlank()) {
            return null;
        }
        Integer possessionHome = null;
        Integer possessionAway = null;
        Integer shotsHome = null;
        Integer shotsAway = null;
        Integer sotHome = null;
        Integer sotAway = null;
        Integer cornersHome = null;
        Integer cornersAway = null;
        Integer offsidesHome = null;
        Integer offsidesAway = null;
        Integer yellowHome = null;
        Integer yellowAway = null;
        Integer redHome = null;
        Integer redAway = null;
        Double xgHome = null;
        Double xgAway = null;

        String section = null;
        for (String record : FlashscoreFeedSupport.splitRecords(statsFeed)) {
            Map<String, String> fields = FlashscoreFeedSupport.parseRecord(record);
            if (fields.containsKey("SE")) {
                section = fields.get("SE");
                continue;
            }
            if (section != null && !section.equalsIgnoreCase("Match")) {
                continue;
            }
            String name = fields.get("SG");
            if (name == null) {
                continue;
            }
            String key = name.toLowerCase(Locale.ROOT).replace('ё', 'е');
            String homeRaw = fields.get("SH");
            String awayRaw = fields.get("SI");
            if (key.contains("ball possession") || key.contains("владение")) {
                possessionHome = parsePercent(homeRaw);
                possessionAway = parsePercent(awayRaw);
            } else if (key.contains("total shots") || key.contains("всего ударов")) {
                shotsHome = parseInt(homeRaw);
                shotsAway = parseInt(awayRaw);
            } else if (key.contains("shots on target") || key.contains("в створ")) {
                sotHome = parseInt(homeRaw);
                sotAway = parseInt(awayRaw);
            } else if (key.contains("corner") || key.contains("углов")) {
                cornersHome = parseInt(homeRaw);
                cornersAway = parseInt(awayRaw);
            } else if (key.contains("offside") || key.contains("офсайд")) {
                offsidesHome = parseInt(homeRaw);
                offsidesAway = parseInt(awayRaw);
            } else if (key.contains("yellow") || key.contains("желт")) {
                yellowHome = parseInt(homeRaw);
                yellowAway = parseInt(awayRaw);
            } else if (key.contains("red card") || key.contains("красн")) {
                redHome = parseInt(homeRaw);
                redAway = parseInt(awayRaw);
            } else if (key.contains("expected goals") || key.equals("xg") || key.contains("ожидаем")) {
                xgHome = parseDouble(homeRaw);
                xgAway = parseDouble(awayRaw);
            }
        }

        if (possessionHome == null && shotsHome == null && sotHome == null
                && cornersHome == null && offsidesHome == null && yellowHome == null && redHome == null
                && xgHome == null) {
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
                .yellowCardsHome(yellowHome)
                .yellowCardsAway(yellowAway)
                .redCardsHome(redHome)
                .redCardsAway(redAway)
                .xgHome(xgHome)
                .xgAway(xgAway)
                .build();
    }

    private static Map<String, String> parseAllRecordFields(String feed) {
        Map<String, String> merged = new java.util.LinkedHashMap<>();
        for (String record : FlashscoreFeedSupport.splitRecords(feed)) {
            merged.putAll(FlashscoreFeedSupport.parseRecord(record));
        }
        return merged;
    }

    private static Map<String, String> firstRecordFields(String feed) {
        List<String> records = FlashscoreFeedSupport.splitRecords(feed);
        if (records.isEmpty()) {
            return Map.of();
        }
        return FlashscoreFeedSupport.parseRecord(records.get(0));
    }

    private static Integer parseInt(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        Matcher m = MINUTE_NUMBER.matcher(raw);
        if (m.find()) {
            try {
                return Integer.parseInt(m.group(1));
            } catch (NumberFormatException ignored) {
                return null;
            }
        }
        return null;
    }

    private static Integer parsePercent(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        String digits = raw.replace("%", "").trim();
        try {
            return Integer.parseInt(digits);
        } catch (NumberFormatException e) {
            return parseInt(raw);
        }
    }

    private static Double parseDouble(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        try {
            return Double.parseDouble(raw.trim().replace(',', '.'));
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
