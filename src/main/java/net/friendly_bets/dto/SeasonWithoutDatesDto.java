package net.friendly_bets.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import net.friendly_bets.models.Season;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SeasonWithoutDatesDto {

    private String seasonId;
    private String title;
    private String status;

    public static SeasonWithoutDatesDto from(Season season) {
        return SeasonWithoutDatesDto.builder()
                .seasonId(season.getId())
                .title(season.getTitle())
                .status(season.getStatus().name())
                .build();
    }
}
