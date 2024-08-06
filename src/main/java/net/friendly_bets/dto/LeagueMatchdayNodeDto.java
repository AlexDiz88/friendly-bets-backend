package net.friendly_bets.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import net.friendly_bets.models.League;
import net.friendly_bets.models.LeagueMatchdayNode;

import java.util.List;
import java.util.stream.Collectors;

@AllArgsConstructor
@NoArgsConstructor
@Builder
@Data
@Schema(description = "Запись календаря тура")
public class LeagueMatchdayNodeDto {

    @Schema(description = "идентификатор лиги", example = "12-байтовый хэш ID")
    private String leagueId;

    @Schema(description = "код лиги", example = "EPL")
    private League.LeagueCode leagueCode;

    @Schema(description = "игровой тур", example = "14")
    private String matchDay;

    @Schema(description = "является ли игра матчем плей-офф", example = "true")
    private Boolean isPlayoff;

    @Schema(description = "раунд плей-офф", example = "1")
    private String playoffRound;

    @Schema(description = "идентификатор лиги", example = "12-байтовый хэш ID")
    private List<BetDto> bets;

    public static LeagueMatchdayNodeDto from(LeagueMatchdayNode node) {
        return LeagueMatchdayNodeDto.builder()
                .leagueId(node.getLeagueId())
                .leagueCode(node.getLeagueCode())
                .matchDay(node.getMatchDay())
                .isPlayoff(node.getIsPlayoff())
                .playoffRound(node.getPlayoffRound())
                .bets(BetDto.from(node.getBets()))
                .build();
    }

    public static List<LeagueMatchdayNodeDto> from(List<LeagueMatchdayNode> nodes) {
        return nodes.stream()
                .map(LeagueMatchdayNodeDto::from)
                .collect(Collectors.toList());
    }

}
