package net.friendly_bets.oddsapi.mapping;

import net.friendly_bets.oddsapi.OddsMarketCatalog;
import net.friendly_bets.oddsapi.OddsMarketCategory;
import org.springframework.stereotype.Component;

import java.util.Locale;

/**
 * Bet365: маппинг только колонки {@code bookmakers["Bet365"]}.
 * Рынки форы (Spread, Asian Handicap) не маппятся — prod-форы только Marathonbet SSE.
 */
@Component
public class Bet365OddsAdapter extends AbstractOddsBookmakerAdapter {

    public static final String BOOKMAKER = "Bet365";

    @Override
    public String bookmakerKey() {
        return BOOKMAKER;
    }

    @Override
    protected boolean invertAwayHandicapSign(String marketName) {
        return true;
    }

    /**
     * Bet365 отдаёт Double Chance текстовыми label («Mexico or Draw»); 1xbet — 1X/12/X2.
     * Маппинг Bet365 DC не нужен: по этим исходам есть 1xbet.
     */
    @Override
    protected OddsMarketCategory resolveCategory(String marketName) {
        if (marketName != null
                && "double chance".equals(marketName.trim().toLowerCase(Locale.ROOT))) {
            return OddsMarketCategory.EXCLUDED;
        }
        return OddsMarketCatalog.resolveCategory(marketName);
    }
}
