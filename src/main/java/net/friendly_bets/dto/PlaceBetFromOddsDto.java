package net.friendly_bets.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.validation.constraints.Min;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class PlaceBetFromOddsDto {

    @NotNull
    @Min(1)
    private Integer wc26ScheduleId;

    @NotBlank
    private String matchDay;

    @NotBlank
    private String selectionKey;

    @NotBlank
    private String bookmaker;

    @NotNull
    private Double clientOdds;
}
