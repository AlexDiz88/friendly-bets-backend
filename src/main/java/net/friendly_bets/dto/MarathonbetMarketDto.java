package net.friendly_bets.dto;

import lombok.Builder;
import lombok.Value;

import java.util.List;

@Value
@Builder
public class MarathonbetMarketDto {
    String model;
    String name;
    List<MarathonbetMarketSelectionDto> selections;
}
