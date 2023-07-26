package net.friendly_bets.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import net.friendly_bets.models.Bet;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@AllArgsConstructor
@NoArgsConstructor
@Builder
@Data
@Schema(description = "Ставка")
public class BetDto {

    @Schema(description = "идентификатор ставки", example = "12-байтовый хэш ID")
    private String id;

    @Schema(description = "имя игрока, сделавшего ставку", example = "Player")
    private String username;

    @Schema(description = "игровой тур", example = "14")
    private String matchDay;

    @Schema(description = "идентификатор матча (от букмекера)", example = "соответствует системе ID у букмекера")
    private String gameId;

    @Schema(description = "дата и время начала матча", example = "2023-07-18T18:31:33")
    private LocalDateTime gameDate;

    @Schema(description = "название команды хозяев", example = "Арсенал")
    private String homeTeamTitle;

    @Schema(description = "название команды гостей", example = "Ливерпуль")
    private String awayTeamTitle;

    @Schema(description = "ставка", example = "П1 + ТМ3,5")
    private String betTitle;

    @Schema(description = "коэффициент ставки", example = "2,14")
    private Double betOdds;

    @Schema(description = "размер ставки", example = "10")
    private Integer betSize;

    @Schema(description = "счет матча", example = "2:1(1:1)")
    private String gameResult;

    @Schema(description = "статус ставки", example = "OPENED")
    private String betStatus;

    @Schema(description = "изменение баланса игрока по результату счёта матча", example = "+21,90")
    private Double balanceChange;


    public static BetDto from(Bet bet) {
        return BetDto.builder()
                .id(bet.getId())
                .username(bet.getUser().getUsername())
                .matchDay(bet.getMatchDay())
                .gameId(bet.getGameId())
                .gameDate(bet.getGameDate())
                .homeTeamTitle(bet.getHomeTeam().getFullTitleRu())
                .awayTeamTitle(bet.getAwayTeam().getFullTitleRu())
                .betTitle(bet.getBetTitle())
                .betOdds(bet.getBetOdds())
                .betSize(bet.getBetSize())
                .gameResult(bet.getGameResult())
                .betStatus(bet.getBetStatus().toString())
                .balanceChange(bet.getBalanceChange())
                .build();
    }

    public static List<BetDto> from(List<Bet> bets) {
        return bets.stream()
                .map(BetDto::from)
                .collect(Collectors.toList());
    }
}
