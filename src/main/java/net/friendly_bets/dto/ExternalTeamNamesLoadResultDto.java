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
public class ExternalTeamNamesLoadResultDto {

    @Builder.Default
    private List<ExternalTeamNameChipDto> unmapped = new ArrayList<>();

    private int autoBoundCount;
    private int mismatchCount;
    /** Existing provider aliases replaced during force sync. */
    private int overwrittenCount;
    private int alreadyMappedCount;
    private int totalNames;
}
