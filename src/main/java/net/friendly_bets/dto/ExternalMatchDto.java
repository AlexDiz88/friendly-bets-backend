package net.friendly_bets.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import net.friendly_bets.models.GameScore;
import net.friendly_bets.models.schedule.MatchGoalEvent;

import java.time.Instant;
import java.util.List;

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
    private Instant utcDate;
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
    private Instant fetchedAt;
    private Instant finalizedAt;
    private String finalizedSource;
    private boolean adminCorrected;
    private boolean finalized;
    /** Live minute label from LIVE provider (e.g. 72'). */
    private String liveMinuteLabel;
    /** Canonical betting slot id (playoff stage detection). */
    private String slotId;
    /** Id матча в wc26_schedule (1–104), если известен. */
    private Integer wc26ScheduleId;
    /** FULL_MATCH goal / red-card / miss events (empty until finalized). */
    private List<MatchGoalEvent> goals;
    private Integer addedTimeFirstHalf;
    private Integer addedTimeSecondHalf;
}
