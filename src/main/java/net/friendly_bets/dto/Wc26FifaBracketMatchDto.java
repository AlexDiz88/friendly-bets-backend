package net.friendly_bets.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Wc26FifaBracketMatchDto {

    private int matchNumber;
    private String stage;
    private String homeFifaCode;
    private String awayFifaCode;
    private String placeholderHome;
    private String placeholderAway;
    private Integer homeScore;
    private Integer awayScore;
    private Integer homePenaltyScore;
    private Integer awayPenaltyScore;
    private String winnerFifaCode;
    private String status;
    private String liveMinuteLabel;
    private LocalDateTime utcDate;
}
