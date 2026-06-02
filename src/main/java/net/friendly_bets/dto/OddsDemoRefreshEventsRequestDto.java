package net.friendly_bets.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotEmpty;
import java.util.List;

@AllArgsConstructor
@NoArgsConstructor
@Builder
@Data
public class OddsDemoRefreshEventsRequestDto {

    @NotBlank
    private String leagueSlug;

    @NotEmpty
    private List<Long> eventIds;
}
