package net.friendly_bets.support;

import lombok.experimental.UtilityClass;
import net.friendly_bets.dto.DeletedBetDto;
import net.friendly_bets.dto.EditedBetDto;
import net.friendly_bets.models.Bet;
import net.friendly_bets.models.BetTitle;
import net.friendly_bets.models.GameScore;

@UtilityClass
public class IntegrationTestDtoFactory {

    public static EditedBetDto editedBetFrom(TestFixture fixture, Bet bet) {
        return editedBetFrom(fixture, bet, bet.getBetStatus(), bet.getBetOdds(), bet.getBetSize(), bet.getGameScore());
    }

    public static EditedBetDto editedBetFrom(TestFixture fixture,
                                             Bet bet,
                                             Bet.BetStatus newStatus,
                                             Double betOdds,
                                             Integer betSize,
                                             GameScore gameScore) {
        return EditedBetDto.builder()
                .seasonId(fixture.getSeason().getId())
                .leagueId(fixture.getLeague().getId())
                .userId(fixture.getPlayer().getId())
                .matchDay(fixture.getMatchDay())
                .homeTeamId(fixture.getHomeTeam().getId())
                .awayTeamId(fixture.getAwayTeam().getId())
                .betTitle(copyBetTitle(bet.getBetTitle()))
                .betOdds(betOdds)
                .betSize(betSize)
                .gameScore(gameScore)
                .betStatus(newStatus.name())
                .calendarNodeId(fixture.getCalendarNode().getId())
                .build();
    }

    public static DeletedBetDto deletedBetFrom(TestFixture fixture) {
        return new DeletedBetDto(
                fixture.getSeason().getId(),
                fixture.getLeague().getId(),
                fixture.getCalendarNode().getId()
        );
    }

    private static BetTitle copyBetTitle(BetTitle source) {
        return BetTitle.builder()
                .code(source.getCode())
                .label(source.getLabel())
                .isNot(source.isNot())
                .build();
    }
}
