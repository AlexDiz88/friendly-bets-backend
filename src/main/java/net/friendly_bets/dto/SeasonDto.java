package net.friendly_bets.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import net.friendly_bets.models.Bet;
import net.friendly_bets.models.League;
import net.friendly_bets.models.Season;
import net.friendly_bets.models.User;

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

    @Schema(description = "название сезона (годы проведения)", example = "2223")
    private String title;

    @Schema(description = "количество ставок на каждый игровой тур", example = "2")
    private Integer betCountPerMatchDay;

    @Schema(description = "статус сезона", example = "ACTIVE")
    private String status;

    @Schema(description = "список игроков, которые участвуют в турнире в этом сезоне", example = "[user1, user2]")
    private List<UserDto> players;

    @Schema(description = "список футбольных лиг, на которые принимаются ставки", example = "[АПЛ, Бундеслига]")
    private List<LeagueDto> leagues;


    public static SeasonDto from(Season season) {
        return SeasonDto.builder()
                .id(season.getId())
                .title(season.getTitle())
                .betCountPerMatchDay(season.getBetCountPerMatchDay())
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
