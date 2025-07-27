package net.friendly_bets.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class GameScores {

    private int homeFullTime;
    private int awayFullTime;

    private int homeFirstHalf;
    private int awayFirstHalf;

    private int homeOverTime;
    private int awayOverTime;

    private int homePenalty;
    private int awayPenalty;
}
