package net.friendly_bets.services;

import net.friendly_bets.dto.NewBetDto;
import net.friendly_bets.exceptions.BadRequestException;
import net.friendly_bets.models.BetTitle;
import net.friendly_bets.models.Season;
import net.friendly_bets.models.enums.BetTitleCode;
import net.friendly_bets.models.schedule.MatchSchedule;
import net.friendly_bets.repositories.MatchScheduleRepository;
import net.friendly_bets.repositories.SeasonsRepository;
import net.friendly_bets.support.AbstractMongoIntegrationTest;
import net.friendly_bets.support.TestDataFactory;
import net.friendly_bets.support.TwoPlayersTestFixture;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

import static net.friendly_bets.support.TestDataFactory.authUser;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class UserAddOpenedBetIntegrationTest extends AbstractMongoIntegrationTest {

    @Autowired
    BetsService betsService;

    @Autowired
    TestDataFactory testData;

    @Autowired
    MatchScheduleRepository matchScheduleRepository;

    @Autowired
    SeasonsRepository seasonsRepository;

    @Test
    @DisplayName("USER addOpenedBet succeeds before match kickoff when schedule exists")
    void userAddOpenedBet_beforeKickoff_succeeds() {
        TwoPlayersTestFixture fx = testData.createTwoPlayersFirstMatchdaySetup(2);
        setSeasonDates(fx.getSeason());
        MatchSchedule schedule = saveScheduledMatch(fx, Instant.now().plus(3, ChronoUnit.HOURS));

        NewBetDto bet = newBetForPlayer(fx, schedule);
        var result = betsService.addOpenedBet(authUser(fx.getPlayerOne()), bet);
        assertNotNull(result.getId());
    }

    @Test
    @DisplayName("USER addOpenedBet rejects after match kickoff")
    void userAddOpenedBet_afterKickoff_rejects() {
        TwoPlayersTestFixture fx = testData.createTwoPlayersFirstMatchdaySetup(2);
        setSeasonDates(fx.getSeason());
        MatchSchedule schedule = saveScheduledMatch(fx, Instant.now().minus(5, ChronoUnit.MINUTES));

        NewBetDto bet = newBetForPlayer(fx, schedule);
        BadRequestException ex = assertThrows(
                BadRequestException.class,
                () -> betsService.addOpenedBet(authUser(fx.getPlayerOne()), bet)
        );
        assertEquals("matchAlreadyStarted", ex.getMessage());
    }

    @Test
    @DisplayName("MODERATOR addOpenedBet allowed after match kickoff")
    void moderatorAddOpenedBet_afterKickoff_succeeds() {
        TwoPlayersTestFixture fx = testData.createTwoPlayersFirstMatchdaySetup(2);
        setSeasonDates(fx.getSeason());
        MatchSchedule schedule = saveScheduledMatch(fx, Instant.now().minus(5, ChronoUnit.MINUTES));

        NewBetDto bet = newBetForPlayer(fx, schedule);
        var result = betsService.addOpenedBet(authUser(fx.getModerator()), bet);
        assertNotNull(result.getId());
    }

    private void setSeasonDates(Season season) {
        season.setStartDate(LocalDate.of(2025, 8, 1));
        season.setEndDate(LocalDate.of(2026, 5, 31));
        seasonsRepository.save(season);
    }

    private MatchSchedule saveScheduledMatch(TwoPlayersTestFixture fx, Instant kickoff) {
        return matchScheduleRepository.save(MatchSchedule.builder()
                .seasonId(fx.getSeason().getId())
                .leagueId(fx.getLeague().getId())
                .leagueCode("EPL")
                .matchday(1)
                .slotId(fx.getMatchDay())
                .status("SCHEDULED")
                .utcKickoff(kickoff)
                .homeTeamId(fx.getHomeTeam().getId())
                .awayTeamId(fx.getAwayTeam().getId())
                .fetchedAt(LocalDateTime.now())
                .build());
    }

    private static NewBetDto newBetForPlayer(TwoPlayersTestFixture fx, MatchSchedule schedule) {
        return NewBetDto.builder()
                .userId(fx.getPlayerOne().getId())
                .seasonId(fx.getSeason().getId())
                .leagueId(fx.getLeague().getId())
                .matchDay(fx.getMatchDay())
                .homeTeamId(fx.getHomeTeam().getId())
                .awayTeamId(fx.getAwayTeam().getId())
                .matchScheduleId(schedule.getId())
                .betTitle(BetTitle.builder()
                        .code(BetTitleCode.HOME_WIN.getCode())
                        .label(BetTitleCode.HOME_WIN.getLabel())
                        .isNot(false)
                        .build())
                .betOdds(2.0)
                .betSize(10)
                .calendarNodeId(fx.getCalendarNode().getId())
                .build();
    }
}
