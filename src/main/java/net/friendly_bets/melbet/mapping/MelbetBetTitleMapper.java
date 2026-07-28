package net.friendly_bets.melbet.mapping;

import com.fasterxml.jackson.databind.JsonNode;
import net.friendly_bets.melbet.MelbetAllowedMarkets;
import net.friendly_bets.melbet.MelbetBookmaker;
import net.friendly_bets.melbet.MelbetMarketBucket;
import net.friendly_bets.models.BetTitle;
import net.friendly_bets.models.enums.BetTitleCode;
import net.friendly_bets.models.odds.OddsLineRow;
import net.friendly_bets.odds.OddsHandicapLine;
import net.friendly_bets.odds.OddsMarketCategory;
import net.friendly_bets.odds.OddsSelectionBetTitleMapper;
import net.friendly_bets.odds.OddsSelectionCode;
import net.friendly_bets.odds.mapping.BetTitleKey;
import net.friendly_bets.odds.mapping.MappedOddsQuote;
import net.friendly_bets.odds.mapping.OddsMappingStatus;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Melbet Digitain StakeTypes → {@link MappedOddsQuote} (deny-by-default allowlist).
 */
@Component
public class MelbetBetTitleMapper {

    private static final Pattern SCORE = Pattern.compile("^(\\d+)\\s*[:\\-]\\s*(\\d+)$");
    private static final Pattern RESULT_TOTAL = Pattern.compile(
            "(П1|П2|Х|X|1X|12|X2|1Х|Х2).*?(ТБ|ТМ|больше|меньше)",
            Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE
    );

    public List<MappedOddsQuote> mapEventPayload(JsonNode body) {
        List<MappedOddsQuote> quotes = new ArrayList<>();
        if (body == null || body.isNull()) {
            return quotes;
        }
        if (body.isArray()) {
            for (int i = 0; i < body.size(); i++) {
                JsonNode part = body.get(i);
                Period period = resolvePeriod(part, i);
                quotes.addAll(mapEventPart(part, period));
            }
        } else {
            quotes.addAll(mapEventPart(body, Period.FULL));
        }
        return dedupeQuotesByBetTitle(quotes);
    }

    private enum Period {
        FULL, FIRST_HALF, SECOND_HALF
    }

    private static Period resolvePeriod(JsonNode part, int index) {
        String epn = text(part, "EPN");
        String pn = text(part, "PN");
        String egn = text(part, "EGN");
        String combined = ((epn != null ? epn : "") + " " + (pn != null ? pn : "") + " " + (egn != null ? egn : ""))
                .toLowerCase(Locale.ROOT);
        if (combined.contains("1st half") || combined.contains("1-й тайм") || combined.contains("1й тайм")) {
            return Period.FIRST_HALF;
        }
        if (combined.contains("2nd half") || combined.contains("2-й тайм") || combined.contains("2й тайм")) {
            return Period.SECOND_HALF;
        }
        if (index == 1) {
            return Period.FIRST_HALF;
        }
        if (index == 2) {
            return Period.SECOND_HALF;
        }
        return Period.FULL;
    }

    private List<MappedOddsQuote> mapEventPart(JsonNode part, Period period) {
        JsonNode stakeTypes = part != null ? part.get("StakeTypes") : null;
        if (stakeTypes == null || !stakeTypes.isArray()) {
            return List.of();
        }
        Map<Integer, MergedStakeType> merged = new LinkedHashMap<>();
        for (JsonNode st : stakeTypes) {
            if (st == null || !st.hasNonNull("Id")) {
                continue;
            }
            int rawId = st.get("Id").asInt();
            if (!MelbetAllowedMarkets.isAllowed(rawId)) {
                continue;
            }
            int canonical = MelbetAllowedMarkets.canonicalStakeTypeId(rawId);
            MergedStakeType bucket = merged.computeIfAbsent(canonical, id -> new MergedStakeType(id, text(st, "N")));
            JsonNode stakes = st.get("Stakes");
            if (stakes == null || !stakes.isArray()) {
                continue;
            }
            for (JsonNode stake : stakes) {
                bucket.stakes.add(stake);
            }
        }
        List<MappedOddsQuote> quotes = new ArrayList<>();
        for (MergedStakeType st : merged.values()) {
            Optional<MelbetMarketBucket> bucket = MelbetAllowedMarkets.bucketFor(st.id);
            if (bucket.isEmpty()) {
                continue;
            }
            quotes.addAll(mapBucket(bucket.get(), st, period));
        }
        return quotes;
    }

    private List<MappedOddsQuote> mapBucket(MelbetMarketBucket bucket, MergedStakeType st, Period period) {
        return switch (bucket) {
            case MATCH_RESULT -> mapMatchResult(st, period);
            case DOUBLE_CHANCE -> mapDoubleChance(st, period);
            case HANDICAP -> mapHandicap(st, period);
            case TOTALS -> mapTotals(st, period);
            case TEAM_TOTAL_HOME -> mapTeamTotals(st, true);
            case TEAM_TOTAL_AWAY -> mapTeamTotals(st, false);
            case BTTS -> mapBtts(st, period);
            case RESULT_BTTS -> mapResultBtts(st);
            case RESULT_TOTAL -> mapResultTotal(st);
            case HALF_FULL -> mapHalfFull(st);
            case FIRST_SECOND_HALF -> mapFirstSecondHalf(st);
            case CORRECT_SCORE -> mapCorrectScore(st, period);
            case GOALS -> mapGoals(st);
            case EXACT_TOTAL_GOALS -> mapExactGoals(st);
            case CLEAN_WIN -> mapCleanWin(st);
            case SCORE_DIFF -> mapScoreDiff(st);
        };
    }

    private List<MappedOddsQuote> mapMatchResult(MergedStakeType st, Period period) {
        List<MappedOddsQuote> quotes = new ArrayList<>();
        for (JsonNode stake : st.stakes) {
            BetTitleCode code = matchResultCode(stakeName(stake), period);
            if (code == null || odds(stake) == null) {
                continue;
            }
            String selectionCode = switch (code) {
                case HOME_WIN, FIRST_HALF_HOME_WIN, SECOND_HALF_HOME_WIN -> "HOME";
                case DRAW, FIRST_HALF_DRAW, SECOND_HALF_DRAW -> "DRAW";
                case AWAY_WIN, FIRST_HALF_AWAY_WIN, SECOND_HALF_AWAY_WIN -> "AWAY";
                default -> null;
            };
            quotes.add(ok(st, stake, OddsMarketCategory.MATCH_RESULT, code, null, selectionCode));
        }
        return quotes;
    }

    private static BetTitleCode matchResultCode(String name, Period period) {
        if (name == null) {
            return null;
        }
        String n = name.trim().toUpperCase(Locale.ROOT);
        boolean home = n.equals("П1") || n.equals("W1") || n.equals("1") || n.equals("WIN1");
        boolean draw = n.equals("X") || n.equals("Х") || n.equals("DRAW");
        boolean away = n.equals("П2") || n.equals("W2") || n.equals("2") || n.equals("WIN2");
        if (period == Period.FIRST_HALF) {
            if (home) return BetTitleCode.FIRST_HALF_HOME_WIN;
            if (draw) return BetTitleCode.FIRST_HALF_DRAW;
            if (away) return BetTitleCode.FIRST_HALF_AWAY_WIN;
            return null;
        }
        if (period == Period.SECOND_HALF) {
            if (home) return BetTitleCode.SECOND_HALF_HOME_WIN;
            if (draw) return BetTitleCode.SECOND_HALF_DRAW;
            if (away) return BetTitleCode.SECOND_HALF_AWAY_WIN;
            return null;
        }
        if (home) return BetTitleCode.HOME_WIN;
        if (draw) return BetTitleCode.DRAW;
        if (away) return BetTitleCode.AWAY_WIN;
        return null;
    }

    private List<MappedOddsQuote> mapDoubleChance(MergedStakeType st, Period period) {
        List<MappedOddsQuote> quotes = new ArrayList<>();
        for (JsonNode stake : st.stakes) {
            OddsSelectionCode dc = doubleChance(stakeName(stake));
            if (dc == null || odds(stake) == null) {
                continue;
            }
            BetTitleCode code = periodDoubleChance(dc, period);
            if (code == null) {
                continue;
            }
            quotes.add(ok(st, stake, OddsMarketCategory.DOUBLE_CHANCE, code, null, dc.name()));
        }
        return quotes;
    }

    private static OddsSelectionCode doubleChance(String name) {
        if (name == null) {
            return null;
        }
        String n = name.trim().toUpperCase(Locale.ROOT).replace('Х', 'X');
        return switch (n) {
            case "1X", "1Х" -> OddsSelectionCode.DC_1X;
            case "12" -> OddsSelectionCode.DC_12;
            case "X2", "Х2" -> OddsSelectionCode.DC_X2;
            default -> null;
        };
    }

    private static BetTitleCode periodDoubleChance(OddsSelectionCode dc, Period period) {
        if (period == Period.FIRST_HALF) {
            return switch (dc) {
                case DC_1X -> BetTitleCode.FIRST_HALF_HOME_WIN_OR_DRAW;
                case DC_12 -> BetTitleCode.FIRST_HALF_HOME_OR_AWAY_WIN;
                case DC_X2 -> BetTitleCode.FIRST_HALF_AWAY_WIN_OR_DRAW;
                default -> null;
            };
        }
        if (period == Period.SECOND_HALF) {
            return switch (dc) {
                case DC_1X -> BetTitleCode.SECOND_HALF_HOME_WIN_OR_DRAW;
                case DC_12 -> BetTitleCode.SECOND_HALF_HOME_OR_AWAY_WIN;
                case DC_X2 -> BetTitleCode.SECOND_HALF_AWAY_WIN_OR_DRAW;
                default -> null;
            };
        }
        return switch (dc) {
            case DC_1X -> BetTitleCode.HOME_WIN_OR_DRAW;
            case DC_12 -> BetTitleCode.HOME_OR_AWAY_WIN;
            case DC_X2 -> BetTitleCode.AWAY_WIN_OR_DRAW;
            default -> null;
        };
    }

    private List<MappedOddsQuote> mapHandicap(MergedStakeType st, Period period) {
        List<MappedOddsQuote> quotes = new ArrayList<>();
        for (JsonNode stake : st.stakes) {
            Double line = argument(stake);
            if (line == null || odds(stake) == null) {
                continue;
            }
            // Skip Asian quarter lines (.25 / .75)
            double absFrac = Math.abs(line) - Math.floor(Math.abs(line));
            if (Math.abs(absFrac - 0.25) < 1e-6 || Math.abs(absFrac - 0.75) < 1e-6) {
                continue;
            }
            boolean home = isHomeHandicap(stake);
            String selectionCode = home ? "HOME" : "AWAY";
            String lineKey = OddsHandicapLine.formatSortKey(line);
            BetTitleCode code = handicapCode(line, home, period);
            if (code == null) {
                continue;
            }
            OddsMarketCategory category = period == Period.FULL
                    ? OddsMarketCategory.HANDICAP
                    : OddsMarketCategory.PERIOD_HANDICAP;
            quotes.add(ok(st, stake, category, code, lineKey, selectionCode));
        }
        return quotes;
    }

    private static boolean isHomeHandicap(JsonNode stake) {
        int sc = stake.path("SC").asInt(0);
        if (sc == 1) {
            return true;
        }
        if (sc == 2) {
            return false;
        }
        String n = stakeName(stake);
        if (n == null) {
            return true;
        }
        String lower = n.toLowerCase(Locale.ROOT);
        return lower.contains("фора1") || lower.startsWith("ф1") || lower.contains("1 (");
    }

    private static BetTitleCode handicapCode(double line, boolean home, Period period) {
        String periodPrefix = period == Period.FIRST_HALF
                ? "FIRST_HALF_"
                : (period == Period.SECOND_HALF ? "SECOND_HALF_" : "");
        String side = home ? "HANDICAP_HOME" : "HANDICAP_AWAY";
        String suffix = handicapSuffix(line);
        try {
            return BetTitleCode.valueOf(periodPrefix + side + suffix);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    private static String handicapSuffix(double effectiveLine) {
        if (Math.abs(effectiveLine) < 1e-9) {
            return "_0";
        }
        String sign = effectiveLine > 0 ? "PLUS" : "MINUS";
        return "_" + sign + "_" + formatLineSuffix(Math.abs(effectiveLine));
    }

    private List<MappedOddsQuote> mapTotals(MergedStakeType st, Period period) {
        List<MappedOddsQuote> quotes = new ArrayList<>();
        for (JsonNode stake : st.stakes) {
            Double line = argument(stake);
            if (line == null || odds(stake) == null) {
                continue;
            }
            double absFrac = Math.abs(line) - Math.floor(Math.abs(line));
            if (Math.abs(absFrac - 0.25) < 1e-6 || Math.abs(absFrac - 0.75) < 1e-6) {
                continue;
            }
            boolean over = isOver(stake);
            String selectionCode = over ? "OVER" : "UNDER";
            String lineKey = formatTotalLine(line);
            BetTitleCode code = totalCode(line, over, period);
            if (code == null) {
                continue;
            }
            OddsMarketCategory category = period == Period.FULL
                    ? OddsMarketCategory.TOTALS
                    : OddsMarketCategory.HALF_TOTALS;
            quotes.add(ok(st, stake, category, code, lineKey, selectionCode));
        }
        return quotes;
    }

    private static BetTitleCode totalCode(double line, boolean over, Period period) {
        String periodPrefix = period == Period.FIRST_HALF
                ? "FIRST_HALF_"
                : (period == Period.SECOND_HALF ? "SECOND_HALF_" : "");
        String name = periodPrefix + "TOTAL_" + (over ? "OVER" : "UNDER") + "_" + formatLineSuffix(line);
        try {
            return BetTitleCode.valueOf(name);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    private List<MappedOddsQuote> mapTeamTotals(MergedStakeType st, boolean home) {
        OddsMarketCategory category = home ? OddsMarketCategory.TEAM_TOTAL_HOME : OddsMarketCategory.TEAM_TOTAL_AWAY;
        List<MappedOddsQuote> quotes = new ArrayList<>();
        for (JsonNode stake : st.stakes) {
            Double line = argument(stake);
            if (line == null || odds(stake) == null) {
                continue;
            }
            String selectionCode = isOver(stake) ? "OVER" : "UNDER";
            String lineKey = formatTotalLine(line);
            try {
                OddsLineRow row = OddsLineRow.builder()
                        .selectionCode(selectionCode)
                        .line(lineKey)
                        .build();
                BetTitle betTitle = OddsSelectionBetTitleMapper.toBetTitle(category.name(), row);
                quotes.add(ok(st, stake, category, betTitle, lineKey, selectionCode));
            } catch (Exception ignored) {
                // unsupported line
            }
        }
        return quotes;
    }

    private List<MappedOddsQuote> mapBtts(MergedStakeType st, Period period) {
        BetTitleCode code = switch (period) {
            case FIRST_HALF -> BetTitleCode.BOTH_TEAMS_SCORE_1ST_HALF;
            case SECOND_HALF -> BetTitleCode.BOTH_TEAMS_SCORE_2ND_HALF;
            case FULL -> BetTitleCode.BOTH_TEAMS_SCORE;
        };
        return mapYesNo(st, code, OddsMarketCategory.BTTS);
    }

    private List<MappedOddsQuote> mapResultBtts(MergedStakeType st) {
        List<MappedOddsQuote> quotes = new ArrayList<>();
        for (JsonNode stake : st.stakes) {
            if (odds(stake) == null) {
                continue;
            }
            BetTitleCode code = resultBttsCode(stakeName(stake));
            if (code == null) {
                continue;
            }
            quotes.add(ok(st, stake, OddsMarketCategory.RESULT_BTTS, code, null, "YES"));
        }
        return quotes;
    }

    private static BetTitleCode resultBttsCode(String name) {
        if (name == null) {
            return null;
        }
        String n = name.toLowerCase(Locale.ROOT);
        boolean bothScore = n.contains("обе забьют") && !n.contains("не забьет") && !n.contains("не забьёт");
        if (!bothScore) {
            return null;
        }
        if (n.contains("п1") || n.startsWith("1 ")) {
            return BetTitleCode.HOME_WIN_AND_BOTH_TEAMS_SCORE;
        }
        if (n.contains("п2") || n.startsWith("2 ")) {
            return BetTitleCode.AWAY_WIN_AND_BOTH_TEAMS_SCORE;
        }
        if (n.contains("х") || n.contains("нич")) {
            return BetTitleCode.DRAW_AND_BOTH_TEAMS_SCORE;
        }
        return null;
    }

    private List<MappedOddsQuote> mapResultTotal(MergedStakeType st) {
        List<MappedOddsQuote> quotes = new ArrayList<>();
        for (JsonNode stake : st.stakes) {
            Double line = argument(stake);
            if (line == null || odds(stake) == null) {
                continue;
            }
            String name = stakeName(stake);
            if (name == null) {
                continue;
            }
            Matcher m = RESULT_TOTAL.matcher(name.replace('Х', 'X'));
            if (!m.find()) {
                continue;
            }
            String resultLeg = m.group(1).toUpperCase(Locale.ROOT).replace('Х', 'X');
            String totalLeg = m.group(2).toLowerCase(Locale.ROOT);
            boolean under = totalLeg.contains("тм") || totalLeg.contains("меньше");
            String prefix = switch (resultLeg) {
                case "П1", "1" -> "HOME_WIN";
                case "П2", "2" -> "AWAY_WIN";
                case "X", "Х" -> "DRAW";
                case "1X", "1Х" -> "HOME_OR_DRAW";
                case "12" -> "HOME_OR_AWAY";
                case "X2", "Х2" -> "AWAY_OR_DRAW";
                default -> null;
            };
            if (prefix == null) {
                continue;
            }
            String enumName = prefix + "_AND_" + (under ? "UNDER" : "OVER") + "_" + formatLineSuffix(line);
            BetTitleCode code;
            try {
                code = BetTitleCode.valueOf(enumName);
            } catch (IllegalArgumentException e) {
                continue;
            }
            OddsMarketCategory category = under
                    ? OddsMarketCategory.RESULT_TOTAL_UNDER
                    : OddsMarketCategory.RESULT_TOTAL_OVER;
            quotes.add(ok(st, stake, category, code, formatTotalLine(line), "YES"));
        }
        return quotes;
    }

    private List<MappedOddsQuote> mapHalfFull(MergedStakeType st) {
        List<MappedOddsQuote> quotes = new ArrayList<>();
        for (JsonNode stake : st.stakes) {
            if (odds(stake) == null) {
                continue;
            }
            BetTitleCode code = halfFullCode(stakeName(stake));
            if (code == null) {
                continue;
            }
            quotes.add(ok(st, stake, OddsMarketCategory.HALF_FULL, code, null, stakeName(stake)));
        }
        return quotes;
    }

    private static BetTitleCode halfFullCode(String name) {
        if (name == null) {
            return null;
        }
        // Melbet samples: П1П1, П1Н, НП1
        String key = name.trim()
                .toUpperCase(Locale.ROOT)
                .replace('Х', 'X')
                .replace("Н", "X")
                .replace(" ", "");
        return switch (key) {
            case "П1П1", "1/1", "11" -> BetTitleCode.HALF_FULL_HOME_HOME;
            case "П1Н", "П1X", "1/X", "1X" -> BetTitleCode.HALF_FULL_HOME_DRAW;
            case "П1П2", "1/2", "12" -> BetTitleCode.HALF_FULL_HOME_AWAY;
            case "НП1", "XП1", "X/1", "X1" -> BetTitleCode.HALF_FULL_DRAW_HOME;
            case "НН", "XX", "X/X" -> BetTitleCode.HALF_FULL_DRAW_DRAW;
            case "НП2", "XП2", "X/2", "X2" -> BetTitleCode.HALF_FULL_DRAW_AWAY;
            case "П2П1", "2/1", "21" -> BetTitleCode.HALF_FULL_AWAY_HOME;
            case "П2Н", "П2X", "2/X", "2X" -> BetTitleCode.HALF_FULL_AWAY_DRAW;
            case "П2П2", "2/2", "22" -> BetTitleCode.HALF_FULL_AWAY_AWAY;
            default -> null;
        };
    }

    private List<MappedOddsQuote> mapFirstSecondHalf(MergedStakeType st) {
        List<MappedOddsQuote> quotes = new ArrayList<>();
        for (JsonNode stake : st.stakes) {
            if (odds(stake) == null) {
                continue;
            }
            BetTitleCode code = firstSecondHalfCode(stakeName(stake));
            if (code == null) {
                continue;
            }
            quotes.add(ok(st, stake, OddsMarketCategory.FIRST_SECOND_HALF, code, null, stakeName(stake)));
        }
        return quotes;
    }

    private static BetTitleCode firstSecondHalfCode(String name) {
        if (name == null) {
            return null;
        }
        // e.g. "1-й тайм X + 2-й тайм П1"
        String[] parts = name.split("\\+");
        if (parts.length != 2) {
            return null;
        }
        String first = resultToken(parts[0]);
        String second = resultToken(parts[1]);
        if (first == null || second == null) {
            return null;
        }
        try {
            return BetTitleCode.valueOf("FIRST_SECOND_" + first + "_" + second);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    private static String resultToken(String segment) {
        if (segment == null) {
            return null;
        }
        String s = segment.toUpperCase(Locale.ROOT).replace('Х', 'X');
        // Prefer explicit Melbet result labels; do not treat the "1" in "1-й тайм" as HOME.
        if (s.contains("П1")) {
            return "HOME";
        }
        if (s.contains("П2")) {
            return "AWAY";
        }
        if (s.contains("X") || s.contains("НИЧ")) {
            return "DRAW";
        }
        Matcher trailing = Pattern.compile("(П?[12]|X)\\s*$").matcher(s.trim());
        if (!trailing.find()) {
            return null;
        }
        return switch (trailing.group(1)) {
            case "П1", "1" -> "HOME";
            case "П2", "2" -> "AWAY";
            case "X" -> "DRAW";
            default -> null;
        };
    }

    private List<MappedOddsQuote> mapCorrectScore(MergedStakeType st, Period period) {
        OddsMarketCategory category = switch (period) {
            case FIRST_HALF -> OddsMarketCategory.FIRST_HALF_CORRECT_SCORE;
            case SECOND_HALF -> OddsMarketCategory.SECOND_HALF_CORRECT_SCORE;
            case FULL -> OddsMarketCategory.CORRECT_SCORE;
        };
        List<MappedOddsQuote> quotes = new ArrayList<>();
        for (JsonNode stake : st.stakes) {
            if (odds(stake) == null) {
                continue;
            }
            Matcher m = SCORE.matcher(stakeName(stake) != null ? stakeName(stake).trim() : "");
            if (!m.matches()) {
                continue;
            }
            int home = Integer.parseInt(m.group(1));
            int away = Integer.parseInt(m.group(2));
            String prefix = period == Period.FIRST_HALF
                    ? "FIRST_HALF_SCORE_"
                    : (period == Period.SECOND_HALF ? "SECOND_HALF_SCORE_" : "GAME_SCORE_");
            BetTitleCode code;
            try {
                code = BetTitleCode.valueOf(prefix + home + "_" + away);
            } catch (IllegalArgumentException e) {
                continue;
            }
            quotes.add(ok(st, stake, category, code, null, home + ":" + away));
        }
        return quotes;
    }

    private List<MappedOddsQuote> mapGoals(MergedStakeType st) {
        BetTitleCode code = switch (st.id) {
            case 27 -> BetTitleCode.HOME_TEAM_SCORES;
            case 28 -> BetTitleCode.AWAY_TEAM_SCORES;
            default -> null;
        };
        if (code == null) {
            return List.of();
        }
        return mapYesNo(st, code, OddsMarketCategory.GOALS);
    }

    private List<MappedOddsQuote> mapExactGoals(MergedStakeType st) {
        // Melbet "Кол-во голов" enumerations — map yes/no style not always available;
        // skip free-form intervals; only exact labels like "2 гола" via EXACT if BetTitle exists.
        return List.of();
    }

    private List<MappedOddsQuote> mapCleanWin(MergedStakeType st) {
        BetTitleCode code = switch (st.id) {
            case 40_393 -> BetTitleCode.CLEAN_WIN_HOME;
            case 40_394 -> BetTitleCode.CLEAN_WIN_AWAY;
            default -> null;
        };
        if (code == null) {
            return List.of();
        }
        return mapYesNo(st, code, OddsMarketCategory.CLEAN_WIN);
    }

    private List<MappedOddsQuote> mapScoreDiff(MergedStakeType st) {
        BetTitleCode code = switch (st.id) {
            case 525 -> BetTitleCode.GOALS_DIFF_HOME_WIN_1;
            case 526 -> BetTitleCode.GOALS_DIFF_HOME_WIN_2;
            case 535 -> BetTitleCode.GOALS_DIFF_AWAY_WIN_1;
            default -> null;
        };
        if (code == null) {
            return List.of();
        }
        return mapYesNo(st, code, OddsMarketCategory.WIN_GOAL_DIFFERENCE);
    }

    private List<MappedOddsQuote> mapYesNo(MergedStakeType st, BetTitleCode code, OddsMarketCategory category) {
        List<MappedOddsQuote> quotes = new ArrayList<>();
        for (JsonNode stake : st.stakes) {
            if (odds(stake) == null) {
                continue;
            }
            String name = stakeName(stake);
            Boolean yes = null;
            if (name != null) {
                if ("Да".equalsIgnoreCase(name.trim()) || "Yes".equalsIgnoreCase(name.trim())) {
                    yes = true;
                } else if ("Нет".equalsIgnoreCase(name.trim()) || "No".equalsIgnoreCase(name.trim())) {
                    yes = false;
                }
            }
            if (yes == null) {
                continue;
            }
            BetTitle betTitle = BetTitle.builder()
                    .code(code.getCode())
                    .label(code.getLabel())
                    .isNot(!yes)
                    .build();
            quotes.add(ok(st, stake, category, betTitle, null, yes ? "YES" : "NO"));
        }
        return quotes;
    }

    private MappedOddsQuote ok(
            MergedStakeType st,
            JsonNode stake,
            OddsMarketCategory category,
            BetTitleCode code,
            String line,
            String selectionCode
    ) {
        BetTitle betTitle = BetTitle.builder()
                .code(code.getCode())
                .label(code.getLabel())
                .isNot(false)
                .build();
        return ok(st, stake, category, betTitle, line, selectionCode);
    }

    private MappedOddsQuote ok(
            MergedStakeType st,
            JsonNode stake,
            OddsMarketCategory category,
            BetTitle betTitle,
            String line,
            String selectionCode
    ) {
        long stakeId = stake.path("Id").asLong(0);
        return MappedOddsQuote.builder()
                .bookmaker(MelbetBookmaker.KEY)
                .marketName(st.name)
                .category(category)
                .betTitle(betTitle)
                .odds(odds(stake))
                .mappingStatus(OddsMappingStatus.OK)
                .selectionCode(selectionCode)
                .line(line)
                .sourcePath("melbet/" + st.id + "/" + stakeId)
                .build();
    }

    private static List<MappedOddsQuote> dedupeQuotesByBetTitle(List<MappedOddsQuote> quotes) {
        Map<BetTitleKey, MappedOddsQuote> byKey = new LinkedHashMap<>();
        List<MappedOddsQuote> withoutKey = new ArrayList<>();
        for (MappedOddsQuote quote : quotes) {
            BetTitleKey key = quote.betTitleKey();
            if (key == null) {
                withoutKey.add(quote);
                continue;
            }
            byKey.put(key, quote);
        }
        List<MappedOddsQuote> result = new ArrayList<>(byKey.values());
        result.addAll(withoutKey);
        return result;
    }

    private static boolean isOver(JsonNode stake) {
        int sc = stake.path("SC").asInt(0);
        if (sc == 1) {
            return true;
        }
        if (sc == 2) {
            return false;
        }
        String n = stakeName(stake);
        if (n == null) {
            return true;
        }
        String lower = n.toLowerCase(Locale.ROOT);
        return lower.contains("больше") || lower.contains("over") || lower.startsWith("тб");
    }

    private static Double argument(JsonNode stake) {
        if (stake == null || !stake.has("A") || stake.get("A").isNull()) {
            return null;
        }
        try {
            return stake.get("A").asDouble();
        } catch (Exception e) {
            return null;
        }
    }

    private static String odds(JsonNode stake) {
        if (stake == null || !stake.has("F") || stake.get("F").isNull()) {
            return null;
        }
        try {
            BigDecimal f = BigDecimal.valueOf(stake.get("F").asDouble()).setScale(3, RoundingMode.HALF_UP);
            return f.stripTrailingZeros().toPlainString();
        } catch (Exception e) {
            return null;
        }
    }

    private static String stakeName(JsonNode stake) {
        return text(stake, "N");
    }

    private static String text(JsonNode node, String field) {
        if (node == null || !node.hasNonNull(field)) {
            return null;
        }
        String v = node.get(field).asText();
        return v != null && !v.isBlank() ? v.trim() : null;
    }

    private static String formatTotalLine(double line) {
        if (line == Math.floor(line)) {
            return String.valueOf((int) line);
        }
        return BigDecimal.valueOf(line).stripTrailingZeros().toPlainString();
    }

    private static String formatLineSuffix(double line) {
        if (line == Math.floor(line)) {
            return ((int) line) + "_0";
        }
        return String.valueOf(line).replace('.', '_');
    }

    private static final class MergedStakeType {
        final int id;
        final String name;
        final List<JsonNode> stakes = new ArrayList<>();

        MergedStakeType(int id, String name) {
            this.id = id;
            this.name = name;
        }
    }
}
