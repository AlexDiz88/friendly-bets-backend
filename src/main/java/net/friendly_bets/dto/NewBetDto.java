package net.friendly_bets.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class NewBetDto {

    private Long userId;
    private String matchDay;
    private Long gameId;
    private LocalDateTime gameDate;
    private Long homeTeamId;
    private Long awayTeamId;
    private String betTitle;
    private Double betOdds;
    private Integer betSize;
}
