package net.friendly_bets.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.validation.constraints.Min;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class NewEmptyBetDto {


    @NotNull(message = "{field.isNull}")
    @NotBlank(message = "{field.isBlank}")
    private String userId;

    @NotNull(message = "{field.isNull}")
    @NotBlank(message = "{field.isBlank}")
    private String seasonId;

    @NotNull(message = "{field.isNull}")
    @NotBlank(message = "{field.isBlank}")
    private String leagueId;

    @NotNull(message = "{field.isNull}")
    @NotBlank(message = "{field.bet.blankMatchDay}")
    private String matchDay;

    @NotNull(message = "{field.bet.betSizeIsNull}")
    @Min(value = 1, message = "{field.bet.betSizeMinValue}")
    private Integer betSize;

    @NotNull(message = "{field.bet.blankCalendarNodeId}")
    @NotBlank(message = "{field.bet.blankCalendarNodeId}")
    private String calendarNodeId;
}
