package net.friendly_bets.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import net.friendly_bets.models.GameScore;
import net.friendly_bets.models.external.ExternalMatch;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExternalMatchDto {

    private long externalMatchId;
    private String competitionCode;
    private int matchday;
    private String season;
    private String status;
    private LocalDateTime utcDate;
    private int homeFootballDataTeamId;
    private int awayFootballDataTeamId;
    private String homeTeamName;
    private String awayTeamName;
    private String homeTeamId;
    private String awayTeamId;
    private String leagueId;
    private GameScore gameScore;
    private LocalDateTime fetchedAt;

    public static ExternalMatchDto from(ExternalMatch match) {
        return ExternalMatchDto.builder()
                .externalMatchId(match.getExternalMatchId())
                .competitionCode(match.getCompetitionCode())
                .matchday(match.getMatchday())
                .season(match.getSeason())
                .status(match.getStatus())
                .utcDate(match.getUtcDate())
                .homeFootballDataTeamId(match.getHomeFootballDataTeamId())
                .awayFootballDataTeamId(match.getAwayFootballDataTeamId())
                .homeTeamName(match.getHomeTeamName())
                .awayTeamName(match.getAwayTeamName())
                .homeTeamId(match.getHomeTeamId())
                .awayTeamId(match.getAwayTeamId())
                .leagueId(match.getLeagueId())
                .gameScore(match.getGameScore())
                .fetchedAt(match.getFetchedAt())
                .build();
    }
}
