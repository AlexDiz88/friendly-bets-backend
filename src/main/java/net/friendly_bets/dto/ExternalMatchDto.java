package net.friendly_bets.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import net.friendly_bets.models.GameScore;
import net.friendly_bets.models.gameresults.GameResultRecord;
import net.friendly_bets.models.gameresults.GameResultSourceSnapshot;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExternalMatchDto {

    private String id;
    private long externalMatchId;
    private String leagueCode;
    private int matchday;
    private String season;
    private String status;
    private LocalDateTime utcDate;
    private String homeTeamName;
    private String awayTeamName;
    private String homeTeamId;
    private String awayTeamId;
    private String homeTeamTitle;
    private String awayTeamTitle;
    private String homeTeamLogoKey;
    private String awayTeamLogoKey;
    private TeamDisplayNamesDto homeTeamDisplayNames;
    private TeamDisplayNamesDto awayTeamDisplayNames;
    private String homeTeamCountry;
    private String awayTeamCountry;
    private String leagueId;
    private GameScore gameScore;
    private LocalDateTime fetchedAt;
    private LocalDateTime finalizedAt;
    private String finalizedSource;
    private boolean adminCorrected;
    private boolean finalized;

    public static ExternalMatchDto from(GameResultRecord match) {
        GameResultSourceSnapshot source = match.footballDataSource();
        long externalMatchId = source != null ? source.getExternalMatchId() : 0L;
        return ExternalMatchDto.builder()
                .id(match.getId())
                .externalMatchId(externalMatchId)
                .leagueCode(match.getLeagueCode())
                .matchday(match.getMatchday())
                .season(match.getSeason())
                .status(match.getStatus())
                .utcDate(match.getUtcDate())
                .homeTeamId(match.getHomeTeamId())
                .awayTeamId(match.getAwayTeamId())
                .leagueId(match.getLeagueId())
                .gameScore(match.getGameScore())
                .fetchedAt(match.getFetchedAt())
                .finalizedAt(match.getFinalizedAt())
                .finalizedSource(match.getFinalizedSource())
                .adminCorrected(match.isAdminCorrected())
                .finalized(match.isFinalized())
                .build();
    }
}
