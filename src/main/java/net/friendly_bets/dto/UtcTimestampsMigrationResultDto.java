package net.friendly_bets.dto;

import lombok.Builder;
import lombok.Value;

import java.util.Map;

@Value
@Builder
public class UtcTimestampsMigrationResultDto {
    Map<String, CollectionStats> collections;
    long accountsTimezoneBackfilled;
    String message;

    @Value
    @Builder
    public static class CollectionStats {
        long scanned;
        long modified;
    }
}
