package net.friendly_bets.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Result of player_stats duplicate cleanup")
public class PlayerStatsDedupResultDto {

    private int groupsScanned;
    private int groupsWithDuplicates;
    private int documentsRemoved;
    private boolean uniqueIndexEnsured;
    private String message;
}
