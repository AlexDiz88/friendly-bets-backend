package net.friendly_bets.oddsapi.mapping;

import net.friendly_bets.models.BetTitle;

@lombok.Value
@lombok.Builder
public class OddsCrossBookmakerMismatch {

    BetTitleKey betTitleKey;
    BetTitle betTitle;
    String bookmakerA;
    String oddsA;
    String bookmakerB;
    String oddsB;
}
