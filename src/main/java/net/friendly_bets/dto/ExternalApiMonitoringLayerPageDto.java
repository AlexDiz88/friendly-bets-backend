package net.friendly_bets.dto;

import lombok.Builder;
import lombok.Value;

import java.util.List;

@Value
@Builder
public class ExternalApiMonitoringLayerPageDto {
    List<ExternalApiMonitoringRunDto> runs;
    long total;
    long failed;
}
