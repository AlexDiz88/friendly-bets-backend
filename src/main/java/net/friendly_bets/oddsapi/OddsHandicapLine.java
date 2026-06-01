package net.friendly_bets.oddsapi;

/**
 * Азиатская фора в API букмекеров (Bet365, 1xbet): поле {@code hdp} задаёт линию хозяев,
 * у гостей — противоположный знак (напр. hdp −2.5 → Ф1(−2.5), Ф2(+2.5)).
 */
public final class OddsHandicapLine {

    private OddsHandicapLine() {
    }

    public static double effectiveLine(String apiLine, boolean home) {
        return effectiveLine(parse(apiLine), home);
    }

    public static double effectiveLine(double apiLine, boolean home) {
        return home ? apiLine : -apiLine;
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

    static double parse(String line) {
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
