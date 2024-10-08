package net.friendly_bets.utils;

import lombok.experimental.UtilityClass;
import net.friendly_bets.models.Bet;

import java.util.List;

@UtilityClass
public class Constants {
    public static final String TOTAL_ID = "total";
    public static final String NO_PREVIOUS_CALENDAR_NODE = "noPreviousCalendarNode";
    public static final String AWS_AVATARS_FOLDER = "avatars";
    public static final String AWS_IMG_FOLDER = "img";
    public static final String AWS_LOCALES_FOLDER = "locales";
    public static final String AWS_LOGO_FOLDER = "logo";
    public static final int MAX_AVATAR_DIMENSION = 150;
    public static final String LANGUAGE_EN = "en";
    public static final String LANGUAGE_DE = "de";
    public static final String LANGUAGE_RU = "ru";
    public static final List<String> SUPPORTED_LANGUAGES = List.of(LANGUAGE_EN, LANGUAGE_DE, LANGUAGE_RU);


    public static final List<Bet.BetStatus> WRL_STATUSES = List.of(
            Bet.BetStatus.WON,
            Bet.BetStatus.RETURNED,
            Bet.BetStatus.LOST
    );

    public static final List<Bet.BetStatus> COMPLETED_BET_STATUSES = List.of(
            Bet.BetStatus.WON,
            Bet.BetStatus.RETURNED,
            Bet.BetStatus.LOST,
            Bet.BetStatus.EMPTY
    );

    public static final List<Bet.BetStatus> VALID_BET_STATUSES = List.of(
            Bet.BetStatus.OPENED,
            Bet.BetStatus.WON,
            Bet.BetStatus.RETURNED,
            Bet.BetStatus.LOST,
            Bet.BetStatus.EMPTY
    );

}
