package net.friendly_bets.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Schema(description = "Страница со списком всех записей календаря сезона")
public class CalendarNodesPage {

    @Schema(description = "Список всех записей календаря")
    private List<CalendarNodeDto> calendarNodes;
}
