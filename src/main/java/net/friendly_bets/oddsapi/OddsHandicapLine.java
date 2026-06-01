package net.friendly_bets.oddsapi;

/**
 * Линия форы из поля {@code hdp} в JSON odds-api.
 * <ul>
 *   <li>1xbet {@code Spread}: хозяева — {@code hdp} как есть; гости — знак инвертируется
 *       ({@code hdp=1} → Ф1(+1) и Ф2(−1) в одной строке).</li>
 *   <li>Bet365 (Spread / Asian Handicap): хозяева — {@code hdp} как есть; гости — знак инвертируется
 *       ({@code hdp=−1.5} → Ф1(−1.5) и Ф2(+1.5)).</li>
 * </ul>
 */
public final class OddsHandicapLine {

    private OddsHandicapLine() {
    }

    /** 1xbet Spread: для гостей знак {@code hdp} инвертируется. */
    public static final boolean INVERT_AWAY_SIGN_XBET_SPREAD = true;

    /** Bet365: колонка {@code away} — зеркальная линия, знак {@code hdp} инвертируется. */
    public static final boolean INVERT_AWAY_SIGN_BET365 = true;

    public static double effectiveLine(String apiLine, boolean home) {
        return effectiveLine(parse(apiLine), home, INVERT_AWAY_SIGN_XBET_SPREAD);
    }

    public static double effectiveLine(double apiLine, boolean home) {
        return effectiveLine(apiLine, home, INVERT_AWAY_SIGN_XBET_SPREAD);
    }

    public static double effectiveLine(String apiLine, boolean home, boolean invertAwaySign) {
        return effectiveLine(parse(apiLine), home, invertAwaySign);
    }

    public static double effectiveLine(double apiLine, boolean home, boolean invertAwaySign) {
        if (home) {
            return apiLine;
        }
        return invertAwaySign ? -apiLine : apiLine;
    }

    /** Формат как в BetTitleCode: {@code -2.5}, {@code +1.5}, {@code 0}. */
    public static String formatSigned(double value) {
        if (Math.abs(value) < 1e-9) {
            return "0";
        }
        if (value == Math.floor(value)) {
            String n = String.valueOf((int) Math.round(value));
            return value > 0 ? "+" + n : n;
        }
        String n = String.valueOf(value);
        return value > 0 ? "+" + n : n;
    }

    public static double parse(String line) {
        if (line == null || line.isBlank()) {
            return 0;
        }
        return Double.parseDouble(line.trim().replace(',', '.'));
    }

    /** Нормализует api-линию: {@code -1.0} → {@code -1}, {@code 2.5} без изменений. */
    public static String canonicalApiLine(String line) {
        if (line == null || line.isBlank()) {
            return line;
        }
        return formatSortKey(parse(line));
    }

    /**
     * Отсекает явно перепутанные котировки (напр. кэф «минусовой» форы на строке «+1»).
     * У плюсовой форы кэф обычно заметно ниже 3; у минусовой — выше ~1.2.
     */
    public static boolean isImplausibleQuote(double effectiveLine, String oddsRaw) {
        if (oddsRaw == null || oddsRaw.isBlank() || "—".equals(oddsRaw.trim())) {
            return false;
        }
        try {
            double odds = Double.parseDouble(oddsRaw.trim().replace(',', '.'));
            if (effectiveLine > 0.01 && odds > 2.8) {
                return true;
            }
            if (effectiveLine < -0.01 && odds < 1.2) {
                return true;
            }
        } catch (NumberFormatException ignored) {
            return false;
        }
        return false;
    }

    /** Числовой ключ для сортировки и слияния строк с одинаковой эффективной форой. */
    public static String formatSortKey(double value) {
        if (Math.abs(value) < 1e-9) {
            return "0";
        }
        if (value == Math.floor(value)) {
            return String.valueOf((int) Math.round(value));
        }
        return String.valueOf(value);
    }
}
