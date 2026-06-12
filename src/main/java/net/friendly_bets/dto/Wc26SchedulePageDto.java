package net.friendly_bets.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Wc26SchedulePageDto {

    private List<Wc26ScheduleMatchDto> matches;
}
