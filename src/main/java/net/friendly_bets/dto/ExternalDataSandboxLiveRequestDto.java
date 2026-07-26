package net.friendly_bets.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExternalDataSandboxLiveRequestDto {
    private String provider;
    /** ISO date YYYY-MM-DD */
    private String date;
    private String titleContains;
}
