package net.friendly_bets.oddsapi.mapping;

import lombok.RequiredArgsConstructor;
import net.friendly_bets.oddsapi.OddsBookmakerKeys;
import net.friendly_bets.oddsapi.OddsMatchContext;
import net.friendly_bets.oddsapi.client.dto.OddsApiMarketDto;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Точка входа пайплайна odds-api: сначала маппинг каждой БК, затем {@link OddsMerger}.
 * См. {@code package-info.java} в этом пакете.
 */
@Component
@RequiredArgsConstructor
public class OddsMappingPipeline {

    private final OddsBookmakerAdapterRegistry adapterRegistry;

    /**
     * @param bookmakerMarkets сырой JSON odds-api: ключ — имя БК, значение — список рынков только этой БК
     * @return результат мержа (группы для UI, rejected, mismatches)
     */
    public OddsMergeResult build(
            Map<String, List<OddsApiMarketDto>> bookmakerMarkets,
            Map<String, String> canonicalByLower,
            OddsMatchContext matchContext
    ) {
        List<MappedOddsQuote> allQuotes = new ArrayList<>();
        if (bookmakerMarkets == null) {
            return OddsMerger.merge(List.of());
        }
        // Этап 1: каждая БК — отдельный адаптер, отдельный JSON, отдельные MappedOddsQuote с BetTitle.
        for (Map.Entry<String, List<OddsApiMarketDto>> entry : bookmakerMarkets.entrySet()) {
            String bookmaker = OddsBookmakerKeys.resolveCanonical(entry.getKey(), canonicalByLower);
            if (bookmaker == null) {
                continue;
            }
            allQuotes.addAll(adapterRegistry.mapBookmaker(bookmaker, entry.getValue(), matchContext));
        }
        // Этап 2: union по BetTitleKey; сверка кэфов между БК; best odds.
        return OddsMerger.merge(allQuotes);
    }
}
