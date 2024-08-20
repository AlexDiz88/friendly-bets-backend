package net.friendly_bets.utils;

import lombok.experimental.UtilityClass;
import net.friendly_bets.models.Bet;

import java.util.List;

@UtilityClass
public class Constants {
    public static final String TOTAL_ID = "total";

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
