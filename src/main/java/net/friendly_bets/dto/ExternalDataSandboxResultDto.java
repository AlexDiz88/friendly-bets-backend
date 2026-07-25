package net.friendly_bets.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExternalDataSandboxResultDto {
    private boolean success;
    private String layer;
    private String provider;
    private Long durationMs;
    private String errorKey;
    private String errorDetail;
    /** Layer-specific structured summary (JSON object). */
    private Object parsed;
    private String rawPayload;
    private boolean rawTruncated;
}
