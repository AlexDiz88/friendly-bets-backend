package net.friendly_bets.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExternalSiteAccessProbeRequestDto {
    /** Absolute http(s) URL of the candidate site home (or any public page). */
    private String url;
}
