package net.friendly_bets.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExternalMatchdayPageDto {

    private ExternalMatchdaySyncDto sync;
    private List<ExternalMatchDto> matches;
}
