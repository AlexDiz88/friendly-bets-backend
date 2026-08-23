package net.friendly_bets.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import net.friendly_bets.models.Season;

import java.time.LocalDate;
import java.util.List;

@AllArgsConstructor
@NoArgsConstructor
@Builder
@Data
@Schema(description = "Сезон для селекторов: без команд, слотов тура, removable и аватаров")
public class SeasonSummaryDto {

    @Schema(description = "идентификатор сезона")
    private String id;

    @Schema(description = "название сезона")
    private String title;

    @Schema(description = "дата начала")
    private LocalDate startDate;

    @Schema(description = "дата окончания")
    private LocalDate endDate;

    @Schema(description = "статус сезона")
    private String status;

    @Schema(description = "участники сезона (id + username, без avatar)")
    private List<UserSimpleDto> players;

    @Schema(description = "лиги сезона без списка команд")
    private List<LeagueSimpleDto> leagues;

    public static SeasonSummaryDto from(Season season) {
        return SeasonSummaryDto.builder()
                .id(season.getId())
                .title(season.getTitle())
                .startDate(season.getStartDate())
                .endDate(season.getEndDate())
                .status(season.getStatus() != null ? season.getStatus().name() : null)
                .players(UserSimpleDto.from(season.getPlayers(), false))
                .leagues(LeagueSimpleDto.from(season.getLeagues()))
                .build();
    }
}
