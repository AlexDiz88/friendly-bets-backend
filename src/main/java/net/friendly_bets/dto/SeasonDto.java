package net.friendly_bets.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import net.friendly_bets.models.Season;
import net.friendly_bets.utils.SeasonCalendarUtils;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

@AllArgsConstructor
@NoArgsConstructor
@Builder
@Data
@Schema(description = "Сезон турнира")
public class SeasonDto {

    @Schema(description = "идентификатор сезона", example = "12-битовый хэш ID")
    private String id;

    @Schema(description = "название сезона (годы проведения)", example = "Season 2023-2024")
    private String title;

    @Schema(description = "дата начала турнира", example = "2024-08-16")
    private LocalDate startDate;

    @Schema(description = "дата окончания турнира", example = "2025-05-25")
    private LocalDate endDate;

    @Schema(description = "год начала сезона для внешней синхронизации", example = "2024")
    private Integer externalSeasonYear;

    @Schema(description = "допустимые годы для запросов к API (от года начала до года конца)")
    private List<Integer> availableExternalYears;

    @Schema(description = "количество ставок на каждый игровой тур", example = "2")
    private Integer betCountPerMatchDay;

    @Schema(description = "размер ставки по умолчанию", example = "10")
    private Integer defaultBetSize;

    @Schema(description = "статус сезона", example = "ACTIVE")
    private String status;

    @Schema(description = "список игроков, которые участвуют в турнире в этом сезоне", example = "[user1, user2]")
    private List<UserDto> players;

    @Schema(description = "список футбольных лиг, на которые принимаются ставки", example = "[АПЛ, Бундеслига]")
    private List<LeagueDto> leagues;


    public static SeasonDto from(Season season) {
        LocalDate start = season.getStartDate();
        LocalDate end = season.getEndDate();
        return SeasonDto.builder()
                .id(season.getId())
                .title(season.getTitle())
                .startDate(start)
                .endDate(end)
                .externalSeasonYear(SeasonCalendarUtils.resolveExternalSeasonYear(start))
                .availableExternalYears(SeasonCalendarUtils.availableExternalYears(start, end))
                .betCountPerMatchDay(season.getBetCountPerMatchDay())
                .defaultBetSize(season.getDefaultBetSize() != null ? season.getDefaultBetSize() : 10)
                .status(season.getStatus().name())
                .players(UserDto.from(season.getPlayers()))
                .leagues(LeagueDto.from(season.getLeagues()))
                .build();
    }

    public static List<SeasonDto> from(List<Season> seasons) {
        return seasons.stream()
                .map(SeasonDto::from)
                .collect(Collectors.toList());
    }
}
