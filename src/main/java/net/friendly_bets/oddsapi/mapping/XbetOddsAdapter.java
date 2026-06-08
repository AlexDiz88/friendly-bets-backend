package net.friendly_bets.oddsapi.mapping;

import org.springframework.stereotype.Component;

/** 1xbet: маппинг только колонки {@code bookmakers["1xbet"]}; Spread/форы odds-api не маппятся. */
@Component
public class XbetOddsAdapter extends AbstractOddsBookmakerAdapter {

    public static final String BOOKMAKER = "1xbet";

    @Override
    public String bookmakerKey() {
        return BOOKMAKER;
    }

    @Override
    protected boolean invertAwayHandicapSign(String marketName) {
        return true;
    }
}
