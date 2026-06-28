package net.friendly_bets.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import net.friendly_bets.models.GameScore;
import net.friendly_bets.models.gameresults.GameResultRecord;
import net.friendly_bets.models.gameresults.GameResultSideSnapshot;
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
    /** Текущая минута live с 4score (напр. 72'). */
    private String liveMinuteLabel;
    /** Id матча в wc26_schedule (1–104), если известен. */
    private Integer wc26ScheduleId;

    public static ExternalMatchDto from(GameResultRecord match) {
        GameResultSourceSnapshot source = match.primaryExternalSource();
        long externalMatchId = source != null ? source.getExternalMatchId() : 0L;
        return ExternalMatchDto.builder()
                .id(match.getId())
                .externalMatchId(externalMatchId)
                .leagueCode(match.getLeagueCode())
                .matchday(match.getMatchday())
                .season(match.getSeason())
                .status(match.getStatus())
                .utcDate(match.getUtcDate())
                .homeTeamName(sideExternalName(source != null ? source.getHome() : null))
                .awayTeamName(sideExternalName(source != null ? source.getAway() : null))
                .homeTeamId(match.getHomeTeamId())
                .awayTeamId(match.getAwayTeamId())
                .leagueId(match.getLeagueId())
                .gameScore(match.getGameScore())
                .fetchedAt(match.getFetchedAt())
                .finalizedAt(match.getFinalizedAt())
                .finalizedSource(match.getFinalizedSource())
                .adminCorrected(match.isAdminCorrected())
                .finalized(match.isFinalized())
                .liveMinuteLabel(match.getLiveMinuteLabel())
                .wc26ScheduleId(match.getWc26ScheduleId())
                .build();
    }

    private static String sideExternalName(GameResultSideSnapshot side) {
        return side != null && side.getExternalName() != null ? side.getExternalName().trim() : null;
    }
}
