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
public class Wc26ScheduleMatchDto {

    private int id;
    private String date;
    private String timeLocal;
    private String venueKey;
    private String stage;
    private String group;
    private String home;
    private String away;
    private String labelKey;
    private LocalDateTime kickoffUtc;
    private String scoreView;
    private String status;
    private boolean finalized;
    private LocalDateTime utcDate;
}
