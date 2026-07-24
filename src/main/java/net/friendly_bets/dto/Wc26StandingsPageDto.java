package net.friendly_bets.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Wc26StandingsPageDto {

    private List<Wc26GroupTableDto> groups;
    private List<Wc26BestThirdRowDto> bestThirdPlaces;
    private LocalDateTime fetchedAt;
}
