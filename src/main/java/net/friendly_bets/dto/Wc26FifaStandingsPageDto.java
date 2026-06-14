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
public class Wc26FifaStandingsPageDto {

    private List<Wc26FifaGroupTableDto> groups;
    private List<Wc26FifaBestThirdRowDto> bestThirdPlaces;
    private LocalDateTime fetchedAt;
    private String sourceUrl;
}
