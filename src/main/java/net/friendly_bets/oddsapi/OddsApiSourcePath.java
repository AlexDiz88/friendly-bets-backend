package net.friendly_bets.oddsapi;

/**
 * Человекочитаемый путь к полю в JSON odds-api: {@code ML.home}, {@code Totals[2.5].over}.
 */
public final class OddsApiSourcePath {

    private OddsApiSourcePath() {
    }

    public static String format(String marketName, String jsonFieldKey, String line) {
        if (marketName == null || marketName.isBlank() || jsonFieldKey == null || jsonFieldKey.isBlank()) {
            return null;
        }
        String market = marketName.trim();
        String field = jsonFieldKey.trim();
        if (line != null && !line.isBlank()) {
            return market + "[" + OddsHandicapLine.canonicalApiLine(line) + "]." + field;
        }
        return market + "." + field;
    }
}
