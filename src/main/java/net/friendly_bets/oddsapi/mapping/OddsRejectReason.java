package net.friendly_bets.oddsapi.mapping;

public enum OddsRejectReason {
    MARKET_EXCLUDED,
    MARKET_UNMAPPED,
    SELECTION_UNMAPPED,
    HANDICAP_ROW_INCOMPLETE,
    BET_TITLE_UNMAPPED,
    CROSS_BOOKMAKER_MISMATCH
}
