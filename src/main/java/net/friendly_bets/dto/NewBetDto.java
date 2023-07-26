package net.friendly_bets.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class NewBetDto {

    private String userId;
    private String matchDay;
    private String gameId;
    private LocalDateTime gameDate;
    private String homeTeamId;
    private String awayTeamId;
    private String betTitle;
    private Double betOdds;
    private Integer betSize;
}
