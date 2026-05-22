package net.friendly_bets.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import net.friendly_bets.models.League;
import net.friendly_bets.models.Season;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LeagueWithoutFormatDto {

    private String leagueId;
    private String leagueCode;
    private String leagueName;
    private String seasonId;
    private String seasonTitle;

    public static LeagueWithoutFormatDto of(Season season, League league) {
        return LeagueWithoutFormatDto.builder()
                .leagueId(league.getId())
                .leagueCode(league.getLeagueCode().toString())
                .leagueName(league.getName())
                .seasonId(season.getId())
                .seasonTitle(season.getTitle())
                .build();
    }
}
