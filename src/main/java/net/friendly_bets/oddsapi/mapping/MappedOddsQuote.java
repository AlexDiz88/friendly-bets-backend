package net.friendly_bets.oddsapi.mapping;

import lombok.Builder;
import lombok.Value;
import net.friendly_bets.models.BetTitle;
import net.friendly_bets.oddsapi.OddsMarketCategory;

/**
 * Одна котировка после этапа 1: уже привязана к одной БК и каноническому {@link BetTitle}.
 * {@link OddsMerger} группирует такие объекты на этапе 2.
 */
@Value
@Builder
public class MappedOddsQuote {

    String bookmaker;
    String marketName;
    String rawRowJson;
    OddsMarketCategory category;
    BetTitle betTitle;
    String odds;
    @Builder.Default
    OddsMappingStatus mappingStatus = OddsMappingStatus.OK;
    OddsRejectReason rejectReason;
    String rejectDetail;
    String selectionCode;
    String line;

    public BetTitleKey betTitleKey() {
        return BetTitleKey.from(betTitle);
    }

    public boolean isOk() {
        return mappingStatus == OddsMappingStatus.OK
                && betTitle != null
                && odds != null
                && !odds.isBlank();
    }
}
