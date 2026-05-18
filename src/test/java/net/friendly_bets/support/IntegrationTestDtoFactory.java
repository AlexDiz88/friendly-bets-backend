package net.friendly_bets.support;

import lombok.experimental.UtilityClass;
import net.friendly_bets.dto.DeletedBetDto;
import net.friendly_bets.dto.EditedBetDto;
import net.friendly_bets.models.Bet;
import net.friendly_bets.models.BetTitle;
import net.friendly_bets.models.GameScore;
import net.friendly_bets.models.Team;
import net.friendly_bets.models.User;
import net.friendly_bets.models.enums.BetTitleCode;

import static net.friendly_bets.support.TestDataFactory.defaultLossGameScore;

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

    public static EditedBetDto editedBetForUser(TwoPlayersTestFixture fixture, Bet bet, User user) {
        return editedBetForUser(fixture, bet, user, bet.getMatchDay());
    }

    public static EditedBetDto editedBetForUser(TwoPlayersTestFixture fixture, Bet bet, User user, String matchDay) {
        return EditedBetDto.builder()
                .seasonId(fixture.getSeason().getId())
                .leagueId(fixture.getLeague().getId())
                .userId(user.getId())
                .matchDay(matchDay)
                .homeTeamId(bet.getHomeTeam().getId())
                .awayTeamId(bet.getAwayTeam().getId())
                .betTitle(copyBetTitle(bet.getBetTitle()))
                .betOdds(bet.getBetOdds())
                .betSize(bet.getBetSize())
                .gameScore(bet.getGameScore())
                .betStatus(bet.getBetStatus().name())
                .calendarNodeId(fixture.getCalendarNode().getId())
                .build();
    }

    public static EditedBetDto editedBetWonToLost(TwoPlayersTestFixture fixture, Bet bet) {
        return editedBetFrom(fixture, bet, bet.getUser(), bet.getMatchDay(),
                bet.getHomeTeam().getId(), bet.getAwayTeam().getId(),
                copyBetTitle(bet.getBetTitle()), bet.getBetOdds(), bet.getBetSize(),
                defaultLossGameScore(), Bet.BetStatus.LOST);
    }

    public static EditedBetDto editedBetWithAwayTeam(TwoPlayersTestFixture fixture, Bet bet, Team newAwayTeam) {
        return editedBetFrom(fixture, bet, bet.getUser(), bet.getMatchDay(),
                bet.getHomeTeam().getId(), newAwayTeam.getId(),
                copyBetTitle(bet.getBetTitle()), bet.getBetOdds(), bet.getBetSize(),
                bet.getGameScore(), bet.getBetStatus());
    }

    public static EditedBetDto editedBetWithMatchDay(TwoPlayersTestFixture fixture, Bet bet, String matchDay) {
        return editedBetForUser(fixture, bet, bet.getUser(), matchDay);
    }

    public static EditedBetDto editedBetWithBetTitle(TwoPlayersTestFixture fixture,
                                                     Bet bet,
                                                     BetTitleCode betTitleCode,
                                                     GameScore gameScore) {
        BetTitle betTitle = BetTitle.builder()
                .code(betTitleCode.getCode())
                .label(betTitleCode.getLabel())
                .isNot(false)
                .build();
        return editedBetFrom(fixture, bet, bet.getUser(), bet.getMatchDay(),
                bet.getHomeTeam().getId(), bet.getAwayTeam().getId(),
                betTitle, bet.getBetOdds(), bet.getBetSize(), gameScore, bet.getBetStatus());
    }

    public static EditedBetDto editedBetWithOdds(TwoPlayersTestFixture fixture, Bet bet, double odds, int size) {
        return editedBetFrom(fixture, bet, bet.getUser(), bet.getMatchDay(),
                bet.getHomeTeam().getId(), bet.getAwayTeam().getId(),
                copyBetTitle(bet.getBetTitle()), odds, size, bet.getGameScore(), bet.getBetStatus());
    }

    private static EditedBetDto editedBetFrom(TwoPlayersTestFixture fixture,
                                              Bet bet,
                                              User user,
                                              String matchDay,
                                              String homeTeamId,
                                              String awayTeamId,
                                              BetTitle betTitle,
                                              double betOdds,
                                              int betSize,
                                              GameScore gameScore,
                                              Bet.BetStatus betStatus) {
        return EditedBetDto.builder()
                .seasonId(fixture.getSeason().getId())
                .leagueId(fixture.getLeague().getId())
                .userId(user.getId())
                .matchDay(matchDay)
                .homeTeamId(homeTeamId)
                .awayTeamId(awayTeamId)
                .betTitle(betTitle)
                .betOdds(betOdds)
                .betSize(betSize)
                .gameScore(gameScore)
                .betStatus(betStatus.name())
                .calendarNodeId(fixture.getCalendarNode().getId())
                .build();
    }

    public static DeletedBetDto deletedBetFrom(TwoPlayersTestFixture fixture) {
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
