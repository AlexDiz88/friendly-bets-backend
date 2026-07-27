package net.friendly_bets.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@AllArgsConstructor
@NoArgsConstructor
@Builder
@Data
public class ExternalTeamNameChipDto {

    private String externalName;
    private String provider;
    private boolean alreadyMapped;
}
