package net.friendly_bets.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@AllArgsConstructor
@NoArgsConstructor
@Builder
@Data
public class OddsDemoRefreshResultDto {

    private String leagueSlug;
    private int eventsStored;
    private List<String> bookmakers;
}
