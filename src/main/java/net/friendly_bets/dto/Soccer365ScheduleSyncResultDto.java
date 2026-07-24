package net.friendly_bets.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@AllArgsConstructor
@NoArgsConstructor
@Builder
@Data
public class Soccer365ScheduleSyncResultDto {

    private String leagueCode;
    private String seasonId;
    private int currentMatchday;
    private Integer nextMatchday;
    private int upserted;
    private int skippedUnmapped;
    private int roundsParsed;
    @Builder.Default
    private List<String> unmappedNames = new ArrayList<>();
}
