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

    @Schema(description = "ID сезона", example = "12-байтовый хэш ID")
    private String seasonId;

    @Schema(description = "ID лиги", example = "12-байтовый хэш ID")
    private String leagueId;

    @Schema(description = "отображаемое имя лиги (EN)", example = "Bundesliga")
    private String leagueDisplayNameEn;

    @Schema(description = "отображаемое имя лиги (RU)", example = "Бундеслига")
    private String leagueDisplayNameRu;

    @Schema(description = "сокращенное имя лиги (EN)", example = "BL")
    private String leagueShortNameEn;

    @Schema(description = "сокращенное имя лиги (RU)", example = "БЛ")
    private String leagueShortNameRu;

    @Schema(description = "время создания/добавления ставки", example = "2023-08-15T12:00:00")
    private LocalDateTime createdAt;

    @Schema(description = "игрок, сделавший ставку", example = "{Player}")
    private UserDto player;

    @Schema(description = "игровой тур", example = "14")
    private String matchDay;

    @Schema(description = "идентификатор матча (от букмекера)", example = "соответствует системе ID у букмекера")
    private String gameId;

    @Schema(description = "дата и время начала матча", example = "2023-07-18T18:31:33")
    private LocalDateTime gameDate;

    @Schema(description = "команда хозяев", example = "{Команда1}")
    private TeamDto homeTeam;

    @Schema(description = "команда гостей", example = "{Команда2}")
    private TeamDto awayTeam;

    @Schema(description = "ставка", example = "П1 + ТМ3,5")
    private String betTitle;

    @Schema(description = "коэффициент ставки", example = "2,14")
    private Double betOdds;

    @Schema(description = "размер ставки", example = "10")
    private Integer betSize;

    @Schema(description = "время добавления результата ставки", example = "2023-08-15T12:00:00")
    private LocalDateTime betResultAddedAt;

    @Schema(description = "счет матча", example = "2:1(1:1)")
    private String gameResult;

    @Schema(description = "статус ставки", example = "OPENED")
    private String betStatus;

    @Schema(description = "изменение баланса игрока по результату счёта матча", example = "+21,90")
    private Double balanceChange;

    @Schema(description = "время изменения/редавтирования/аннулирования ставки", example = "2023-08-15T12:00:00")
    private LocalDateTime updatedAt;


    public static BetDto from(Bet bet) {
        if (bet.getBetStatus().equals(Bet.BetStatus.EMPTY) || bet.getBetStatus().equals(Bet.BetStatus.DELETED)) {
            return BetDto.builder()
                    .id(bet.getId())
                    .seasonId(bet.getSeason().getId())
                    .leagueId(bet.getLeague().getId())
                    .leagueDisplayNameEn(bet.getLeague().getDisplayNameEn())
                    .leagueDisplayNameRu(bet.getLeague().getDisplayNameRu())
                    .leagueShortNameEn(bet.getLeague().getShortNameEn())
                    .leagueShortNameRu(bet.getLeague().getShortNameRu())
                    .createdAt(bet.getCreatedAt())
                    .player(UserDto.from(bet.getUser()))
                    .matchDay(bet.getMatchDay())
                    .betSize(bet.getBetSize())
                    .gameResult(bet.getGameResult())
                    .betResultAddedAt(bet.getBetResultAddedAt())
                    .betStatus(bet.getBetStatus().toString())
                    .balanceChange(bet.getBalanceChange())
                    .updatedAt(bet.getUpdatedAt())
                    .build();
        }
        return BetDto.builder()
                .id(bet.getId())
                .seasonId(bet.getSeason().getId())
                .leagueId(bet.getLeague().getId())
                .leagueDisplayNameEn(bet.getLeague().getDisplayNameEn())
                .leagueDisplayNameRu(bet.getLeague().getDisplayNameRu())
                .leagueShortNameEn(bet.getLeague().getShortNameEn())
                .leagueShortNameRu(bet.getLeague().getShortNameRu())
                .createdAt(bet.getCreatedAt())
                .player(UserDto.from(bet.getUser()))
                .matchDay(bet.getMatchDay())
                .gameId(bet.getGameId())
                .gameDate(bet.getGameDate())
                .homeTeam(TeamDto.from(bet.getHomeTeam()))
                .awayTeam(TeamDto.from(bet.getAwayTeam()))
                .betTitle(bet.getBetTitle())
                .betOdds(bet.getBetOdds())
                .betSize(bet.getBetSize())
                .betResultAddedAt(bet.getBetResultAddedAt())
                .gameResult(bet.getGameResult())
                .betStatus(bet.getBetStatus().toString())
                .balanceChange(bet.getBalanceChange())
                .updatedAt(bet.getUpdatedAt())
                .build();
    }

    public static List<BetDto> from(List<Bet> bets) {
        return bets.stream()
                .map(BetDto::from)
                .collect(Collectors.toList());
    }
}
