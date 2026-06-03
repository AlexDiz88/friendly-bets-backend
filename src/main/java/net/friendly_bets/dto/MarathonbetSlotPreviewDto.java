package net.friendly_bets.dto;

import lombok.Builder;
import lombok.Value;

import java.util.List;

@Value
@Builder
public class MarathonbetSlotPreviewDto {
    String leagueId;
    String leagueCode;
    String season;
    int matchday;
    long tournamentTreeId;
    List<MarathonbetSlotMatchPreviewDto> matches;
}
