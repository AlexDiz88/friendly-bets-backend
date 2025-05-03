package net.friendly_bets.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import net.friendly_bets.models.League;
import net.friendly_bets.models.LeagueMatchdayNode;

import java.util.ArrayList;
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

    @Schema(description = "лимит ставок на тур", example = "2")
    private Integer betCountLimit;

    @Schema(description = "идентификатор лиги", example = "12-байтовый хэш ID")
    private List<BetDto> bets;

    public static LeagueMatchdayNodeDto from(LeagueMatchdayNode node, boolean isWithBets) {
        return LeagueMatchdayNodeDto.builder()
                .leagueId(node.getLeagueId())
                .leagueCode(node.getLeagueCode())
                .matchDay(node.getMatchDay())
                .betCountLimit(node.getBetCountLimit())
                .bets(isWithBets ? BetDto.from(node.getBets()) : new ArrayList<>())
                .build();
    }

    public static List<LeagueMatchdayNodeDto> from(List<LeagueMatchdayNode> nodes, boolean isWithBets) {
        return nodes.stream()
                .map(node -> LeagueMatchdayNodeDto.from(node, isWithBets))
                .collect(Collectors.toList());
    }

}
