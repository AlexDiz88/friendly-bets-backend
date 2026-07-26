package net.friendly_bets.dto;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class MatchScheduleExternalIdsMigrationResultDto {
    long matched;
    long modified;
    String message;
}
