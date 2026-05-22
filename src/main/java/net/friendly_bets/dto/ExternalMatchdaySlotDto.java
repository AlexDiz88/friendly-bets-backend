package net.friendly_bets.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExternalMatchdaySlotDto {

    /** Порядковый номер слота в формате (1..N), ключ кэша external sync. */
    private int value;
    /** Канонический id тура = Bet.match_day */
    private String slotId;
    /** Отображение: id тура или ключ playoffStage */
    private String label;
    private String kind;
}
