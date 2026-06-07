package net.friendly_bets.dto;

import lombok.Builder;
import lombok.Value;

import java.util.List;

@Value
@Builder
public class MarathonbetMarketDto {
    Long marketId;
    String model;
    String name;
    List<MarathonbetMarketSelectionDto> selections;
    /** Не мержится в prod (напр. «Победа с учётом форы (3 исхода)»). */
    boolean ignoredForProd;
}
