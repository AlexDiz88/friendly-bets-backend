package net.friendly_bets.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Wc26FifaBestThirdRowDto {

    private int rank;
    private String group;
    private String fifaCode;
    private int played;
    private int wins;
    private int draws;
    private int losses;
    private int points;
    private int goalDifference;
    private int goalsFor;
    private int goalsAgainst;
    private boolean qualifies;
}
