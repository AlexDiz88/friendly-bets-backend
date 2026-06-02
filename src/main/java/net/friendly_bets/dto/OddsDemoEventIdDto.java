package net.friendly_bets.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import net.friendly_bets.oddsapi.client.dto.OddsApiEventDto;

@AllArgsConstructor
@NoArgsConstructor
@Builder
@Data
public class OddsDemoEventIdDto {

    private Long id;
    private String home;
    private String away;
    private String date;
    private String status;

    public static OddsDemoEventIdDto from(OddsApiEventDto event) {
        return OddsDemoEventIdDto.builder()
                .id(event.getId())
                .home(event.getHome())
                .away(event.getAway())
                .date(event.getDate())
                .status(event.getStatus())
                .build();
    }
}
