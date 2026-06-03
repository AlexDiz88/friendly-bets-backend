package net.friendly_bets.dto;

import lombok.Builder;
import lombok.Value;

import java.math.BigDecimal;

@Value
@Builder
public class MarathonbetMarketSelectionDto {
    Long selId;
    String name;
    BigDecimal odds;
}
