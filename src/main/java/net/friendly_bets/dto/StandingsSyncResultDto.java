package net.friendly_bets.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@NoArgsConstructor
@AllArgsConstructor
@Builder
@Data
public class StandingsSyncResultDto {

    private String leagueCode;
    private String seasonId;
    private String leagueId;
    private String provider;
    private int rowsSaved;
    private int skippedUnmapped;
    @Builder.Default
    private List<String> unmappedNames = new ArrayList<>();
}
