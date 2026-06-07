package net.friendly_bets.oddsapi.mapping;

import net.friendly_bets.oddsapi.OddsMatchContext;
import net.friendly_bets.oddsapi.client.dto.OddsApiMarketDto;

import java.util.List;

/**
 * Маппинг сырого JSON одной букмекерской колонки в канонические {@link net.friendly_bets.models.BetTitle}.
 * Реализации не должны читать данные других БК — кросс-букмекерная логика только в {@link OddsMerger}.
 */
public interface OddsBookmakerAdapter {

    String bookmakerKey();

    /** Этап 1: все рынки этой БК → котировки с заполненным {@code betTitle}. */
    List<MappedOddsQuote> mapMarkets(List<OddsApiMarketDto> markets, OddsMatchContext matchContext);
}
