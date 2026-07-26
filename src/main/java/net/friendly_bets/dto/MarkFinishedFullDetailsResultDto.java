package net.friendly_bets.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MarkFinishedFullDetailsResultDto {

    private long matchedCount;
    private long modifiedCount;
    private Instant fullDetailsFetchedAt;
}
