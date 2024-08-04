package net.friendly_bets.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import net.friendly_bets.models.LeagueMatchdayNode;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import java.time.LocalDate;
import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class NewCalendarNodeDto {

    @NotNull(message = "{field.isNull}")
    @NotBlank(message = "{field.seasonId.isBlank}")
    private String seasonId;

    @NotNull(message = "{field.calendar.startDateIsNull}")
    private LocalDate startDate;

    @NotNull(message = "{field.calendar.endDateIsNull}")
    private LocalDate endDate;

    @NotNull(message = "{field.calendar.list.isNull}")
    @Size(min = 1, message = "{field.calendar.list.size}")
    private List<LeagueMatchdayNode> leagueMatchdayNodes;
}
