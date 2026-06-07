package net.friendly_bets.oddsapi.mapping;

import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

/**
 * Реестр адаптеров: одна реализация {@link OddsBookmakerAdapter} на каноническое имя БК.
 * Маппинг всегда идёт через {@link #mapBookmaker}, без общего парсера «на всех сразу».
 */
@Component
public class OddsBookmakerAdapterRegistry {

    private final Map<String, OddsBookmakerAdapter> byCanonicalKey;

    public OddsBookmakerAdapterRegistry(Bet365OddsAdapter bet365, XbetOddsAdapter xbet) {
        byCanonicalKey = new LinkedHashMap<>();
        register(bet365);
        register(xbet);
    }

    private void register(OddsBookmakerAdapter adapter) {
        byCanonicalKey.put(adapter.bookmakerKey().toLowerCase(Locale.ROOT), adapter);
    }

    public Optional<OddsBookmakerAdapter> resolve(String canonicalBookmaker) {
        if (canonicalBookmaker == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(byCanonicalKey.get(canonicalBookmaker.toLowerCase(Locale.ROOT)));
    }

    /**
     * Этап 1 пайплайна: только рынки одной БК → список {@link MappedOddsQuote} с каноническими BetTitle.
     */
    public List<MappedOddsQuote> mapBookmaker(
            String canonicalBookmaker,
            List<net.friendly_bets.oddsapi.client.dto.OddsApiMarketDto> markets,
            net.friendly_bets.oddsapi.OddsMatchContext context
    ) {
        return resolve(canonicalBookmaker)
                .map(adapter -> adapter.mapMarkets(markets, context))
                .orElse(List.of());
    }
}
