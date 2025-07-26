package net.friendly_bets.utils;

import net.friendly_bets.dto.EditedBetDto;
import net.friendly_bets.dto.NewBetDto;
import net.friendly_bets.dto.NewEmptyBet;
import net.friendly_bets.exceptions.BadRequestException;
import net.friendly_bets.exceptions.ConflictException;
import net.friendly_bets.models.*;
import net.friendly_bets.repositories.BetsRepository;
import net.friendly_bets.repositories.LeaguesRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Stream;

import static org.junit.Assert.assertThrows;
import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.params.provider.Arguments.arguments;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BetUtilsTest {

    @Mock
    private BetsRepository betsRepository;

    @Mock
    private LeaguesRepository leaguesRepository;

    @Test
    @DisplayName("Constructor should be private")
    void constructor_ShouldBePrivate() throws NoSuchMethodException {
        Constructor<BetUtils> constructor = BetUtils.class.getDeclaredConstructor();
        constructor.setAccessible(true);
        assertThrows(InvocationTargetException.class, constructor::newInstance);
    }

    @ParameterizedTest
    @EnumSource(value = Bet.BetStatus.class, names = {"OPENED", "EMPTY", "DELETED"})
    @DisplayName("Should do nothing when bet status is not in WRL_STATUSES")
    void checkGameResult_ShouldDoNothing_WhenBetStatusIsNotWRLStatus(Bet.BetStatus betStatus) {
        // given
        GameResult nullGameResult = null;
        GameResult normalGameResult = new GameResult("2:1", "1:0", null, null);

        // when + then
        assertDoesNotThrow(() -> BetUtils.checkGameResult(nullGameResult, betStatus));
        assertDoesNotThrow(() -> BetUtils.checkGameResult(normalGameResult, betStatus));
    }

    @ParameterizedTest
    @EnumSource(value = Bet.BetStatus.class, names = {"WON", "RETURNED", "LOST"})
    @DisplayName("Should throw BadRequestException when game result is null and bet status is in WRL_STATUSES")
    void checkGameResult_ShouldThrowException_WhenGameResultIsNullAndBetStatusIsWRLStatus(Bet.BetStatus betStatus) {
        // given
        GameResult gameResult = null;

        // when + then
        BadRequestException exception = assertThrows(BadRequestException.class,
                () -> BetUtils.checkGameResult(gameResult, betStatus));

        assertEquals("gameResultIsNull", exception.getMessage());
    }

    @ParameterizedTest
    @MethodSource("provideInvalidGameResults")
    @DisplayName("Should throw BadRequestException when GameResult has invalid fullTime or firstTime")
    void checkGameResult_ShouldThrowException_WhenGameResultIsInvalid(GameResult gameResult) {
        // given
        Bet.BetStatus betStatus = Bet.BetStatus.WON;

        // when + then
        BadRequestException exception = assertThrows(BadRequestException.class,
                () -> BetUtils.checkGameResult(gameResult, betStatus));

        assertEquals("incorrectGameResult", exception.getMessage());
    }

    private static Stream<GameResult> provideInvalidGameResults() {
        return Stream.of(
                GameResult.builder().fullTime(null).firstTime(null).build(),
                GameResult.builder().fullTime("1-0").firstTime(null).build(),
                GameResult.builder().fullTime(null).firstTime("2-1").build(),
                GameResult.builder().fullTime("1-0").firstTime("").build(),
                GameResult.builder().fullTime("").firstTime("2-1").build(),
                GameResult.builder().fullTime("1-0").firstTime("  ").build(),
                GameResult.builder().fullTime(" ").firstTime("2-1").build());
    }

    @ParameterizedTest
    @MethodSource("validGameResultsProvider")
    @DisplayName("Should not throw exception when valid game result and valid bet status are provided")
    void checkGameResult_ShouldPass_WhenValidGameResultAndValidBetStatusAreProvided(GameResult validGameResult) {
        Bet.BetStatus betStatus = Bet.BetStatus.WON;

        assertDoesNotThrow(() -> BetUtils.checkGameResult(validGameResult, betStatus));
    }

    private static Stream<GameResult> validGameResultsProvider() {
        return Stream.of(
                new GameResult("3:1", "1:1", null, null),
                new GameResult("2:2", "2:2", null, null),
                new GameResult("3:3", "0:0", null, null),
                new GameResult("0:0", "0:0", null, null),
                new GameResult("1:1", "0:1", "2:1", null),
                new GameResult("1:1", "0:0", "0:1", null),
                new GameResult("3:1", "1:1", "2:0", null),
                new GameResult("2:5", "2:0", "1:0", null),
                new GameResult("0:0", "0:0", "0:0", "4:3"),
                new GameResult("2:2", "2:0", "1:1", "3:5"),
                new GameResult("3:1", "1:1", "0:0", "4:2"),
                new GameResult("1:3", "0:1", "1:1", "3:0")
        );
    }

    @ParameterizedTest
    @MethodSource("invalidGameResultsProvider")
    @DisplayName("Should throw BadRequestException when invalid game result and valid bet status")
    void shouldFail_WhenFirstHalfScoreHigherThanFullTimeScore(GameResult invalidGameResult) {
        // given
        Bet.BetStatus betStatus = Bet.BetStatus.WON;

        // when + then
        BadRequestException exception = assertThrows(BadRequestException.class,
                () -> BetUtils.checkGameResult(invalidGameResult, betStatus));

        assertEquals("incorrectGameResult", exception.getMessage());
    }

    private static Stream<GameResult> invalidGameResultsProvider() {
        return Stream.of(
                new GameResult("1 1", "1:1", null, null),
                new GameResult("1-1", "1:1", null, null),
                new GameResult("1:1!", "1:1", null, null),
                new GameResult("1: 1", "1:1", null, null),
                new GameResult(":1:1", "1:1", null, null),
                new GameResult("0:01", "0:0", null, null),
                new GameResult("3::1", "1:1", null, null),
                new GameResult("test", "1:1", null, null),
                new GameResult("0:0", "1:1", null, null),
                new GameResult("0:0", "1:1", null, "1:3"),
                new GameResult("1:1", "1:1", "1:1", null),
                new GameResult("3:1", "2:0", "0:0", null),
                new GameResult("3:3", "1:2", "1:1", "3:3"),
                new GameResult("2:0", "0:0", "1:1", "4:0"),
                new GameResult("3:3", "1:2", "1:3", "3:3"),
                new GameResult("0:1", "1:2", "2:1", "7:3")
        );
    }

    // ------------------------------------------------------------------------------------------------------ //

    @Test
    @DisplayName("Should not throw exception when valid bet is provided")
    void validateBet_ShouldPass_WhenValidBetProvided() {
        // given
        NewBetDto validBet = NewBetDto.builder()
                .homeTeamId("teamId-1")
                .awayTeamId("teamId-2")
                .betOdds(2.25)
                .build();

        // when + then
        assertDoesNotThrow(() -> BetUtils.validateBet(validBet));
    }

    @Test
    @DisplayName("Should throw BadRequestException when home team is the same as away team")
    void validateBet_ShouldFail_WhenHomeTeamIsEqualToAwayTeam() {
        // given
        NewBetDto invalidBet = NewBetDto.builder()
                .homeTeamId("teamId")
                .awayTeamId("teamId")
                .betOdds(2.25)
                .build();

        // when + then
        BadRequestException exception = assertThrows(BadRequestException.class,
                () -> BetUtils.validateBet(invalidBet));

        assertEquals("homeTeamCannotBeEqualAwayTeam", exception.getMessage());
    }

    @ParameterizedTest
    @MethodSource("invalidBetOddsProvider")
    @DisplayName("Should throw exception when bet odds are invalid")
    void validateBet_ShouldFail_WhenBetOddsAreInvalid(NewBetDto invalidBet, String expectedErrorMessage) {
        // when + then
        BadRequestException exception = assertThrows(BadRequestException.class,
                () -> BetUtils.validateBet(invalidBet));

        assertEquals(expectedErrorMessage, exception.getMessage());
    }

    private static Stream<Arguments> invalidBetOddsProvider() {
        return Stream.of(
                Arguments.of(NewBetDto.builder()
                        .homeTeamId("1").awayTeamId("2")
                        .betOdds(Double.NaN).build(), "betCoefIsNotNumber"),
                Arguments.of(NewBetDto.builder()
                        .homeTeamId("1").awayTeamId("2")
                        .betOdds(1.0).build(), "betCoefCannotBeLessThan"),
                Arguments.of(NewBetDto.builder()
                        .homeTeamId("1").awayTeamId("2")
                        .betOdds(0.5).build(), "betCoefCannotBeLessThan"),
                Arguments.of(NewBetDto.builder()
                        .homeTeamId("1").awayTeamId("2")
                        .betOdds(-2.25).build(), "betCoefCannotBeLessThan")
        );
    }

    // ------------------------------------------------------------------------------------------------------ //

    @Test
    @DisplayName("Should throw ConflictException when bet already exists")
    void checkIfBetAlreadyExists_ShouldThrowConflictException_WhenBetExists() {
        // given
        NewBetDto newBetDto = new NewBetDto();

        when(betsRepository.existsBySeason_IdAndLeague_IdAndUser_IdAndMatchDayAndHomeTeam_IdAndAwayTeam_IdAndBetStatusIn(
                any(), any(), any(), any(), any(), any(), any()))
                .thenReturn(true);

        // when + then
        ConflictException exception = assertThrows(ConflictException.class,
                () -> BetUtils.checkIfBetAlreadyExists(betsRepository, newBetDto));

        assertEquals("betAlreadyAdded", exception.getMessage());
    }

    @Test
    @DisplayName("Should not throw exception when bet does not exist")
    void checkIfBetAlreadyExists_ShouldNotThrow_WhenBetDoesNotExist() {
        // given
        NewBetDto newBetDto = new NewBetDto();

        when(betsRepository.existsBySeason_IdAndLeague_IdAndUser_IdAndMatchDayAndHomeTeam_IdAndAwayTeam_IdAndBetStatusIn(
                any(), any(), any(), any(), any(), any(), any()))
                .thenReturn(false);

        // when + then
        assertDoesNotThrow(() -> BetUtils.checkIfBetAlreadyExists(betsRepository, newBetDto));
    }

    // ------------------------------------------------------------------------------------------------------ //

    @Test
    @DisplayName("Should throw ConflictException when bet already edited")
    void checkIfBetAlreadyEdited_ShouldThrowConflictException_WhenBetExists() {
        // given
        EditedBetDto editedBet = new EditedBetDto();
        BetTitle betTitle = new BetTitle();
        editedBet.setBetTitle(betTitle);
        Bet.BetStatus betStatus = Bet.BetStatus.OPENED;

        when(betsRepository.existsBySeason_IdAndLeague_IdAndUser_IdAndMatchDayAndHomeTeam_IdAndAwayTeam_IdAndBetTitle_CodeAndBetTitle_IsNotAndBetOddsAndBetSizeAndGameResultAndBetStatus(
                any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any()))
                .thenReturn(true);

        // when + then
        ConflictException exception = assertThrows(ConflictException.class,
                () -> BetUtils.checkIfBetAlreadyEdited(betsRepository, editedBet, betStatus));

        assertEquals("betAlreadyEdited", exception.getMessage());
    }

    @Test
    @DisplayName("Should not throw exception when bet has not been edited")
    void checkIfBetAlreadyEdited_ShouldNotThrow_WhenBetDoesNotExist() {
        // given
        EditedBetDto editedBet = new EditedBetDto();
        BetTitle betTitle = new BetTitle();
        editedBet.setBetTitle(betTitle);
        Bet.BetStatus betStatus = Bet.BetStatus.OPENED;

        when(betsRepository.existsBySeason_IdAndLeague_IdAndUser_IdAndMatchDayAndHomeTeam_IdAndAwayTeam_IdAndBetTitle_CodeAndBetTitle_IsNotAndBetOddsAndBetSizeAndGameResultAndBetStatus(
                any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any()))
                .thenReturn(false);

        // when + then
        assertDoesNotThrow(() -> BetUtils.checkIfBetAlreadyEdited(betsRepository, editedBet, betStatus));
    }

    // ------------------------------------------------------------------------------------------------------ //

    @Test
    @DisplayName("Should create a new opened bet with the given parameters")
    void createNewOpenedBet_ShouldCreateBet_WhenValidParametersProvided() {
        // given
        String expectedMatchDay = "matchDay";
        BetTitle expectedBetTitle = BetTitle.builder()
                .code((short) 101)
                .label("betLabel")
                .isNot(false)
                .build();
        double expectedBetOdds = 2.5;
        int expectedBetSize = 10;
        String expectedCalendarNodeId = "calendarNodeId";
        Bet.BetStatus expectedBetStatus = Bet.BetStatus.OPENED;

        NewBetDto newOpenedBet = NewBetDto.builder()
                .matchDay(expectedMatchDay)
                .betTitle(expectedBetTitle)
                .betOdds(expectedBetOdds)
                .betSize(expectedBetSize)
                .calendarNodeId(expectedCalendarNodeId)
                .build();

        User expectedModerator = new User();
        User expectedUser = new User();
        Season expectedSeason = new Season();
        League expectedLeague = new League();
        Team expectedHomeTeam = new Team();
        Team expectedAwayTeam = new Team();

        // when
        Bet actualResult = BetUtils.createNewOpenedBet(newOpenedBet, expectedModerator, expectedUser, expectedSeason, expectedLeague, expectedHomeTeam, expectedAwayTeam);

        // then
        assertNotNull(actualResult);
        assertNotNull(actualResult.getCreatedAt());
        assertEquals(expectedModerator, actualResult.getCreatedBy());
        assertEquals(expectedUser, actualResult.getUser());
        assertEquals(expectedSeason, actualResult.getSeason());
        assertEquals(expectedLeague, actualResult.getLeague());
        assertEquals(expectedMatchDay, actualResult.getMatchDay());
        assertEquals(expectedHomeTeam, actualResult.getHomeTeam());
        assertEquals(expectedAwayTeam, actualResult.getAwayTeam());
        assertEquals(expectedBetTitle, actualResult.getBetTitle());
        assertEquals(expectedBetOdds, actualResult.getBetOdds());
        assertEquals(expectedBetSize, actualResult.getBetSize());
        assertEquals(expectedBetStatus, actualResult.getBetStatus());
        assertEquals(expectedCalendarNodeId, actualResult.getCalendarNodeId());
    }

    @Test
    @DisplayName("Should create a new empty bet with the given parameters")
    void createNewEmptyBet_ShouldCreateBet_WhenValidParametersProvided() {
        // given
        String expectedMatchDay = "matchDay";
        int expectedBetSize = 10;
        String expectedCalendarNodeId = "calendarNodeId";
        Bet.BetStatus expectedBetStatus = Bet.BetStatus.EMPTY;

        NewEmptyBet newEmptyBet = NewEmptyBet.builder()
                .matchDay(expectedMatchDay)
                .betSize(expectedBetSize)
                .calendarNodeId(expectedCalendarNodeId)
                .build();

        User expectedModerator = new User();
        User expectedUser = new User();
        Season expectedSeason = new Season();
        League expectedLeague = new League();

        // when
        Bet actualResult = BetUtils.createNewEmptyBet(newEmptyBet, expectedModerator, expectedUser, expectedSeason, expectedLeague);

        // then
        assertNotNull(actualResult);
        assertNotNull(actualResult.getCreatedAt());
        assertEquals(expectedModerator, actualResult.getCreatedBy());
        assertEquals(expectedUser, actualResult.getUser());
        assertEquals(expectedSeason, actualResult.getSeason());
        assertEquals(expectedLeague, actualResult.getLeague());
        assertEquals(expectedMatchDay, actualResult.getMatchDay());
        assertEquals(expectedBetSize, actualResult.getBetSize());
        assertNotNull(actualResult.getBetResultAddedAt());
        assertEquals(expectedBetStatus, actualResult.getBetStatus());
        assertEquals(-(double) expectedBetSize, actualResult.getBalanceChange());
        assertEquals(expectedCalendarNodeId, actualResult.getCalendarNodeId());
    }

    // ------------------------------------------------------------------------------------------------------ //

    @Test
    @DisplayName("Should throw BadRequestException when bet status is EMPTY or DELETED")
    void getPreviousStateOfBet_ShouldThrowBadRequestException_WhenBetIsDeleted() {
        // given
        Bet emptyBet = Bet.builder().betStatus(Bet.BetStatus.EMPTY).build();
        Bet deletedBet = Bet.builder().betStatus(Bet.BetStatus.DELETED).build();

        // when + then
        BadRequestException exception1 = assertThrows(BadRequestException.class,
                () -> BetUtils.getPreviousStateOfBet(emptyBet));
        BadRequestException exception2 = assertThrows(BadRequestException.class,
                () -> BetUtils.getPreviousStateOfBet(deletedBet));

        assertEquals("emptyAndDeletedBetsCannotBeEdited", exception1.getMessage());
        assertEquals("emptyAndDeletedBetsCannotBeEdited", exception2.getMessage());
    }

    @Test
    @DisplayName("Should return a new bet with previous state when bet status is not EMPTY or DELETED")
    void getPreviousStateOfBet_ShouldReturnPreviousStateBet_WhenBetStatusValid() {
        // given
        BetTitle originalBetTitle = BetTitle.builder()
                .code((short) 101)
                .label("betLabel")
                .isNot(false)
                .build();

        Bet originalBet = Bet.builder()
                .user(new User())
                .matchDay("matchDay")
                .homeTeam(new Team())
                .awayTeam(new Team())
                .betTitle(originalBetTitle)
                .betOdds(1.75)
                .betSize(10)
                .betStatus(Bet.BetStatus.WON)
                .gameResult(new GameResult())
                .balanceChange(7.5)
                .build();

        // when
        Bet previousStateBet = BetUtils.getPreviousStateOfBet(originalBet);

        // then
        assertNotNull(previousStateBet);
        assertEquals(originalBet.getUser(), previousStateBet.getUser());
        assertEquals(originalBet.getMatchDay(), previousStateBet.getMatchDay());
        assertEquals(originalBet.getHomeTeam(), previousStateBet.getHomeTeam());
        assertEquals(originalBet.getAwayTeam(), previousStateBet.getAwayTeam());
        assertEquals(originalBet.getBetTitle(), previousStateBet.getBetTitle());
        assertEquals(originalBet.getBetOdds(), previousStateBet.getBetOdds());
        assertEquals(originalBet.getBetSize(), previousStateBet.getBetSize());
        assertEquals(originalBet.getBetStatus(), previousStateBet.getBetStatus());
        assertEquals(originalBet.getGameResult(), previousStateBet.getGameResult());
        assertEquals(originalBet.getBalanceChange(), previousStateBet.getBalanceChange());
    }

    // ------------------------------------------------------------------------------------------------------ //

    @Test
    @DisplayName("Should throw BadRequestException when season has no players")
    void updateLeagueCurrentMatchDay_ShouldThrowBadRequestException_WhenNoPlayersInSeason() {
        // given
        Season season1 = Season.builder().players(null).build();
        Season season2 = Season.builder().players(Collections.emptyList()).build();
        League league = new League();

        // when + then
        BadRequestException exception1 = assertThrows(BadRequestException.class,
                () -> BetUtils.updateLeagueCurrentMatchDay(leaguesRepository, betsRepository, season1, league));
        BadRequestException exception2 = assertThrows(BadRequestException.class,
                () -> BetUtils.updateLeagueCurrentMatchDay(leaguesRepository, betsRepository, season2, league));

        assertEquals("noPlayersInSeason", exception1.getMessage());
        assertEquals("noPlayersInSeason", exception2.getMessage());
    }

    @Test
    @DisplayName("Should throw BadRequestException when season has null or zero bet count per match day")
    void updateLeagueCurrentMatchDay_ShouldThrowBadRequestException_WhenNullOrZeroBetCountPerMatchDay() {
        // given
        Season season1 = Season.builder()
                .players(Arrays.asList(new User(), new User()))
                .betCountPerMatchDay(null)
                .build();

        Season season2 = Season.builder()
                .players(Arrays.asList(new User(), new User()))
                .betCountPerMatchDay(0)
                .build();

        League league = new League();

        // when + then
        BadRequestException exception1 = assertThrows(BadRequestException.class,
                () -> BetUtils.updateLeagueCurrentMatchDay(leaguesRepository, betsRepository, season1, league));
        BadRequestException exception2 = assertThrows(BadRequestException.class,
                () -> BetUtils.updateLeagueCurrentMatchDay(leaguesRepository, betsRepository, season2, league));

        assertEquals("nullOrZeroBetCountPerMatchDay", exception1.getMessage());
        assertEquals("nullOrZeroBetCountPerMatchDay", exception2.getMessage());
    }

    @Test
    @DisplayName("Should update league current match day when calculation is not equal current matchday")
    void updateLeagueCurrentMatchDay_ShouldUpdateLeagueCurrentMatchDay_WhenCalculationIsDifferent() {
        // given
        Season season = Season.builder()
                .players(Arrays.asList(new User(), new User()))
                .betCountPerMatchDay(2)
                .build();

        League league = League.builder().currentMatchDay("1").build();

        when(betsRepository.countBetsByLeagueAndBetStatusNot(league, Bet.BetStatus.DELETED)).thenReturn(5);

        // when
        BetUtils.updateLeagueCurrentMatchDay(leaguesRepository, betsRepository, season, league);

        // then
        assertEquals("2", league.getCurrentMatchDay());
        verify(betsRepository, times(1)).countBetsByLeagueAndBetStatusNot(league, Bet.BetStatus.DELETED);
        verify(leaguesRepository, times(1)).save(league);
    }

    @Test
    @DisplayName("Should not update league current match day when calculation is the same")
    void updateLeagueCurrentMatchDay_ShouldNotUpdateLeagueCurrentMatchDay_WhenCalculationIsSame() {
        // given
        Season season = Season.builder()
                .players(Arrays.asList(new User(), new User()))
                .betCountPerMatchDay(2)
                .build();

        League league = League.builder().currentMatchDay("2").build();

        when(betsRepository.countBetsByLeagueAndBetStatusNot(league, Bet.BetStatus.DELETED)).thenReturn(7);

        // when
        BetUtils.updateLeagueCurrentMatchDay(leaguesRepository, betsRepository, season, league);

        // then
        assertEquals("2", league.getCurrentMatchDay());
        verify(betsRepository, times(1)).countBetsByLeagueAndBetStatusNot(league, Bet.BetStatus.DELETED);
        verify(leaguesRepository, never()).save(any());
    }

    // ------------------------------------------------------------------------------------------------------ //

    @Test
    @DisplayName("Should throw BadRequestException when user exceeds the bet limit for the match day")
    void checkLeagueBetLimit_ShouldThrowBadRequestException_WhenUserExceedsBetLimit() {
        // given
        String userId = "userId";
        User user = User.builder().id(userId).build();

        LeagueMatchdayNode node = LeagueMatchdayNode.builder()
                .betCountLimit(2)
                .bets(Arrays.asList(
                        Bet.builder().user(user).build(),
                        Bet.builder().user(user).build()))
                .build();

        // when + then
        BadRequestException exception = assertThrows(BadRequestException.class,
                () -> BetUtils.checkLeagueBetLimit(node, userId));

        assertEquals("exceededLimitBetsFromPlayer", exception.getMessage());
    }

    @Test
    @DisplayName("Should not throw an exception when user is within the bet limit")
    void checkLeagueBetLimit_ShouldNotThrowException_WhenUserWithinBetLimit() {
        // given
        String userId = "userId";
        User user = User.builder().id(userId).build();

        LeagueMatchdayNode node = LeagueMatchdayNode.builder()
                .betCountLimit(2)
                .bets(Collections.singletonList(Bet.builder().user(user).build()))
                .build();

        // when + then
        assertDoesNotThrow(() -> BetUtils.checkLeagueBetLimit(node, userId));
    }

    // ------------------------------------------------------------------------------------------------------ //

    @ParameterizedTest
    @MethodSource("provideBetResultValues")
    @DisplayName("Should correctly process bet result and update balance change")
    void processBetResultValues_ShouldUpdateBalanceChangeAndBetResult(Bet.BetStatus betStatus, int betSize, double betOdds, double expectedBalanceChange) {
        // given
        User moderator = new User();
        GameResult gameResult = new GameResult();

        Bet bet = Bet.builder()
                .betSize(betSize)
                .betOdds(betOdds)
                .build();

        BetResult betResult = BetResult.builder()
                .betStatus(betStatus.name())
                .gameResult(gameResult)
                .build();

        // when
        BetUtils.processBetResultValues(moderator, bet, betResult);

        // then
        assertEquals(expectedBalanceChange, bet.getBalanceChange());
        assertNotNull(bet.getBetResultAddedAt());
        assertEquals(moderator, bet.getBetResultAddedBy());
        assertEquals(betStatus, bet.getBetStatus());
        assertEquals(gameResult, bet.getGameResult());
    }

    static Stream<Arguments> provideBetResultValues() {
        return Stream.of(
                arguments(Bet.BetStatus.WON, 10, 2.5, 15.0),
                arguments(Bet.BetStatus.LOST, 10, 2.5, -10.0),
                arguments(Bet.BetStatus.RETURNED, 10, 2.5, 0.0)
        );
    }

    // ------------------------------------------------------------------------------------------------------ //

    @ParameterizedTest
    @MethodSource("provideEditedBetValues")
    @DisplayName("Should update edited bet values correctly for WRL bet statuses")
    void updateEditedBetValues_ShouldUpdateEditedBetValuesCorrectly(Integer newBetSize, Double newBetOdds,
                                                                    Bet.BetStatus newBetStatus, Double expectedBalanceChange) {
        // given
        String expectedBetId = "betId";
        Season season = Season.builder().id("seasonId").build();
        League league = League.builder().id("leagueId").build();
        User moderator = new User();


        Bet bet = Bet.builder()
                .id(expectedBetId)
                .season(season)
                .league(league)
                .user(new User())
                .matchDay("previousMatchday")
                .homeTeam(new Team())
                .awayTeam(new Team())
                .betTitle(new BetTitle())
                .betSize(10)
                .betOdds(2.2)
                .gameResult(new GameResult())
                .betStatus(Bet.BetStatus.WON)
                .build();

        User newUser = User.builder().id("newUserId").build();
        Team newHomeTeam = Team.builder().id("newHomeTeamId").build();
        Team newAwayTeam = Team.builder().id("newAwayTeamId").build();

        String newMatchday = "newMatchday";
        BetTitle newBetTitle = BetTitle.builder()
                .code((short) 101)
                .label("betLabel")
                .isNot(true)
                .build();
        GameResult newGameResult = GameResult.builder().fullTime("2:2").firstTime("1:1").build();

        EditedBetDto editedBet = EditedBetDto.builder()
                .matchDay(newMatchday)
                .betTitle(newBetTitle)
                .betSize(newBetSize)
                .betOdds(newBetOdds)
                .gameResult(newGameResult)
                .betStatus(newBetStatus.toString())
                .build();

        // when
        BetUtils.updateEditedBetValues(betsRepository, bet, editedBet, moderator, newUser, newHomeTeam, newAwayTeam);

        // then
        assertEquals(expectedBetId, bet.getId());
        assertEquals(season, bet.getSeason());
        assertEquals(league, bet.getLeague());

        assertNotNull(bet.getUpdatedAt());
        assertEquals(moderator, bet.getUpdatedBy());

        assertEquals(newUser, bet.getUser());
        assertEquals(newMatchday, bet.getMatchDay());
        assertEquals(newHomeTeam, bet.getHomeTeam());
        assertEquals(newAwayTeam, bet.getAwayTeam());
        assertEquals(newBetTitle, bet.getBetTitle());
        assertEquals(newBetOdds, bet.getBetOdds());
        assertEquals(newBetSize, bet.getBetSize());
        assertEquals(newBetStatus, bet.getBetStatus());

        assertEquals(newGameResult, bet.getGameResult());
        assertEquals(expectedBalanceChange, bet.getBalanceChange());
    }

    static Stream<Arguments> provideEditedBetValues() {
        return Stream.of(
                arguments(10, 2.5, Bet.BetStatus.WON, 15.0),
                arguments(20, 2.5, Bet.BetStatus.LOST, -20.0),
                arguments(15, 2.5, Bet.BetStatus.RETURNED, 0.0)
        );
    }

    @ParameterizedTest
    @MethodSource("provideNonWrlBetValues")
    @DisplayName("Should update edited bet values correctly but not update balance and game result for non-WRL bet statuses")
    void updateEditedBetValues_ShouldNotUpdateBalanceOrGameResultForNonWrlStatuses(Integer newBetSize, Double newBetOdds, Bet.BetStatus newBetStatus) {
        // given
        String expectedBetId = "betId";
        Season season = Season.builder().id("seasonId").build();
        League league = League.builder().id("leagueId").build();
        User moderator = new User();

        Bet bet = Bet.builder()
                .id(expectedBetId)
                .season(season)
                .league(league)
                .user(new User())
                .matchDay("previousMatchday")
                .homeTeam(new Team())
                .awayTeam(new Team())
                .betTitle(new BetTitle())
                .betStatus(Bet.BetStatus.OPENED)
                .build();

        User newUser = User.builder().id("newUserId").build();
        Team newHomeTeam = Team.builder().id("newHomeTeamId").build();
        Team newAwayTeam = Team.builder().id("newAwayTeamId").build();

        String newMatchday = "newMatchday";
        BetTitle newBetTitle = BetTitle.builder()
                .code((short) 101)
                .label("betLabel")
                .isNot(true)
                .build();

        EditedBetDto editedBet = EditedBetDto.builder()
                .matchDay(newMatchday)
                .betTitle(newBetTitle)
                .betSize(newBetSize)
                .betOdds(newBetOdds)
                .betStatus(newBetStatus.toString())
                .build();

        // when
        BetUtils.updateEditedBetValues(betsRepository, bet, editedBet, moderator, newUser, newHomeTeam, newAwayTeam);

        // then
        assertEquals(expectedBetId, bet.getId());
        assertEquals(season, bet.getSeason());
        assertEquals(league, bet.getLeague());

        assertNotNull(bet.getUpdatedAt());
        assertEquals(moderator, bet.getUpdatedBy());

        assertEquals(newUser, bet.getUser());
        assertEquals(newMatchday, bet.getMatchDay());
        assertEquals(newHomeTeam, bet.getHomeTeam());
        assertEquals(newAwayTeam, bet.getAwayTeam());
        assertEquals(newBetTitle, bet.getBetTitle());
        assertEquals(newBetOdds, bet.getBetOdds());
        assertEquals(newBetSize, bet.getBetSize());

        assertNull(bet.getBalanceChange());
        assertNull(bet.getGameResult());
    }

    static Stream<Arguments> provideNonWrlBetValues() {
        return Stream.of(
                arguments(10, 2.5, Bet.BetStatus.OPENED),
                arguments(10, null, Bet.BetStatus.EMPTY),
                arguments(15, 3.4, Bet.BetStatus.DELETED)
        );
    }

    // ------------------------------------------------------------------------------------------------------ //

    @ParameterizedTest
    @MethodSource("provideDeletedBetValues")
    @DisplayName("Should update deleted bet values and reset balance for completed bet statuses")
    void updateDeletedBetValues_ShouldUpdateValuesAndResetBalanceForCompletedStatuses(Bet.BetStatus status,
                                                                                      Double initialBalanceChange, Double expectedBalanceChange) {
        // given
        Bet bet = Bet.builder()
                .betStatus(status)
                .balanceChange(initialBalanceChange)
                .build();

        User moderator = new User();

        // when
        BetUtils.updateDeletedBetValues(bet, moderator);

        // then
        assertNotNull(bet.getUpdatedAt());
        assertEquals(moderator, bet.getUpdatedBy());
        assertEquals(expectedBalanceChange, bet.getBalanceChange());
    }

    static Stream<Arguments> provideDeletedBetValues() {
        return Stream.of(
                arguments(Bet.BetStatus.WON, 25.0, 0.0),
                arguments(Bet.BetStatus.LOST, 25.0, 0.0),
                arguments(Bet.BetStatus.RETURNED, 25.0, 0.0),
                arguments(Bet.BetStatus.EMPTY, 25.0, 0.0),
                arguments(Bet.BetStatus.OPENED, 25.0, 25.0),
                arguments(Bet.BetStatus.DELETED, 25.0, 25.0)
        );
    }

    // ------------------------------------------------------------------------------------------------------ //

    @ParameterizedTest
    @MethodSource("provideDatesForValidation")
    @DisplayName("Should validate date range correctly or throw ConflictException")
    void datesRangeValidation_ShouldValidateOrThrowConflictException(LocalDate startDate, LocalDate endDate, boolean shouldThrowException) {
        // when + then
        if (shouldThrowException) {
            ConflictException exception = assertThrows(ConflictException.class, () -> BetUtils.datesRangeValidation(startDate, endDate));
            assertEquals("startDateMustBeBeforeOrEqualToEndDate", exception.getMessage());
        } else {
            assertDoesNotThrow(() -> BetUtils.datesRangeValidation(startDate, endDate));
        }
    }

    static Stream<Arguments> provideDatesForValidation() {
        return Stream.of(
                arguments(LocalDate.of(2024, 1, 1), LocalDate.of(2024, 1, 2), false),
                arguments(LocalDate.of(2024, 1, 1), LocalDate.of(2024, 1, 1), false),
                arguments(LocalDate.of(2024, 1, 2), LocalDate.of(2024, 1, 1), true)
        );
    }

    // ------------------------------------------------------------------------------------------------------ //

    @ParameterizedTest
    @MethodSource("provideMatchdayNodes")
    @DisplayName("Should validate league matchdays or throw ConflictException")
    void leagueMatchdaysValidation_ShouldValidateOrThrowConflictException(List<LeagueMatchdayNode> matchdayNodes,
                                                                          boolean shouldThrowException,
                                                                          LeagueMatchdayNode firstConflictNode) {
        // given
        CalendarNode calendarNode1 = CalendarNode.builder()
                .leagueMatchdayNodes(List.of(
                        LeagueMatchdayNode.builder().leagueId("league1").matchDay("1").build(),
                        LeagueMatchdayNode.builder().leagueId("league2").matchDay("1").build()))
                .build();

        CalendarNode calendarNode2 = CalendarNode.builder()
                .leagueMatchdayNodes(List.of(
                        LeagueMatchdayNode.builder().leagueId("league1").matchDay("2").build(),
                        LeagueMatchdayNode.builder().leagueId("league2").matchDay("2").build()))
                .build();

        List<CalendarNode> existingCalendarNodes = List.of(calendarNode1, calendarNode2);

        // when + then
        if (shouldThrowException) {
            ConflictException exception = assertThrows(ConflictException.class,
                    () -> BetUtils.leagueMatchdaysValidation(existingCalendarNodes, matchdayNodes));
            assertEquals("Выбранная лига с указанным туром уже добавлена в календарь - " + firstConflictNode.getLeagueCode() + " " + firstConflictNode.getMatchDay(), exception.getMessage());
        } else {
            assertDoesNotThrow(() -> BetUtils.leagueMatchdaysValidation(existingCalendarNodes, matchdayNodes));
        }
    }

    static Stream<Arguments> provideMatchdayNodes() {
        // have conflict
        LeagueMatchdayNode newNode1 = LeagueMatchdayNode.builder()
                .leagueId("league1")
                .matchDay("1")
                .leagueCode(League.LeagueCode.EPL)
                .build();

        LeagueMatchdayNode newNode2 = LeagueMatchdayNode.builder()
                .leagueId("league2")
                .matchDay("2")
                .leagueCode(League.LeagueCode.BL)
                .build();

        // have no conflict
        LeagueMatchdayNode newNode3 = LeagueMatchdayNode.builder()
                .leagueId("league1")
                .matchDay("3")
                .leagueCode(League.LeagueCode.EPL)
                .build();

        LeagueMatchdayNode newNode4 = LeagueMatchdayNode.builder()
                .leagueId("league3")
                .matchDay("1")
                .leagueCode(League.LeagueCode.CL)
                .build();

        return Stream.of(
                // Cases with conflict
                arguments(List.of(newNode1), true, newNode1),
                arguments(List.of(newNode2), true, newNode2),
                arguments(List.of(newNode1, newNode2), true, newNode1),
                arguments(List.of(newNode1, newNode3), true, newNode1),
                arguments(List.of(newNode1, newNode4), true, newNode1),
                arguments(List.of(newNode2, newNode3), true, newNode2),
                arguments(List.of(newNode2, newNode4), true, newNode2),
                arguments(List.of(newNode4, newNode2), true, newNode2),
                // Cases with no conflict
                arguments(List.of(newNode3), false, null),
                arguments(List.of(newNode4), false, null),
                arguments(List.of(newNode3, newNode4), false, null),
                arguments(List.of(newNode4, newNode3), false, null)
        );
    }

}
