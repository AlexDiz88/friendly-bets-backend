package net.friendly_bets.utils;

import java.util.regex.Pattern;

public class GameResultValidator {

    private static final String SCORE_PATTERN = "^\\d+:\\d+ \\(\\d+:\\d+\\)$";

    public static boolean isValidGameResult(String score) {
        return Pattern.matches(SCORE_PATTERN, score);
    }
}
