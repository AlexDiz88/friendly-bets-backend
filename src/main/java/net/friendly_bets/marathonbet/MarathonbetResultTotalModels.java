package net.friendly_bets.marathonbet;

import java.util.regex.Pattern;

/**
 * Модели Marathonbet «исход + тотал» (полный матч), напр. {@code MTCH_T1WOV}, {@code MTCH_T1ORT2WUND}.
 */
public final class MarathonbetResultTotalModels {

    private static final Pattern FULL_TIME = Pattern.compile(
            "^MTCH_(T1W|T2W|X|T1WD|T2WD|T1ORT2W)(UND|OV)$"
    );

    private MarathonbetResultTotalModels() {
    }

    public static boolean isFullTimeResultTotal(String model) {
        return model != null && FULL_TIME.matcher(model).matches();
    }

    public enum ResultLeg {
        HOME_WIN,
        AWAY_WIN,
        DRAW,
        HOME_OR_DRAW,
        AWAY_OR_DRAW,
        HOME_OR_AWAY
    }

    public static ResultLeg resultLeg(String model) {
        if (model == null) {
            return null;
        }
        var m = FULL_TIME.matcher(model);
        if (!m.matches()) {
            return null;
        }
        return switch (m.group(1)) {
            case "T1W" -> ResultLeg.HOME_WIN;
            case "T2W" -> ResultLeg.AWAY_WIN;
            case "X" -> ResultLeg.DRAW;
            case "T1WD" -> ResultLeg.HOME_OR_DRAW;
            case "T2WD" -> ResultLeg.AWAY_OR_DRAW;
            case "T1ORT2W" -> ResultLeg.HOME_OR_AWAY;
            default -> null;
        };
    }

    public static boolean isUnder(String model) {
        if (model == null) {
            return false;
        }
        return model.endsWith("UND");
    }
}
