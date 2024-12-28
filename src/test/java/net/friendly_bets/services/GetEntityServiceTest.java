package net.friendly_bets.services;

import net.friendly_bets.exceptions.BadRequestException;
import net.friendly_bets.exceptions.NotFoundException;
import net.friendly_bets.models.*;
import net.friendly_bets.repositories.*;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.params.provider.Arguments.arguments;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class GetEntityServiceTest {

    @Mock
    private UsersRepository usersRepository;
    @Mock
    private SeasonsRepository seasonsRepository;
    @Mock
    private LeaguesRepository leaguesRepository;
    @Mock
    private TeamsRepository teamsRepository;
    @Mock
    private BetsRepository betsRepository;
    @Mock
    private CalendarsRepository calendarsRepository;
    @Mock
    private PlayerStatsRepository playerStatsRepository;
    @Mock
    private PlayerStatsByTeamsRepository playerStatsByTeamsRepository;

    @InjectMocks
    private GetEntityService getEntityService;


    private final String userId = "userId";
    private final String seasonId = "seasonId";
    private final String leagueId = "leagueId";
    private final String teamId = "teamId";
    private final String betId = "betId";
    private final String calendarNodeId = "calendarNodeId";

    @Test
    @DisplayName("Should return User when exists")
    void getUserOrThrow_ShouldReturnUser_WhenExists() {
        testEntityGetOrThrow(usersRepository, userId, new User(), getEntityService::getUserOrThrow);
    }

    @Test
    @DisplayName("Should throw NotFoundException when user not found")
    void getUserOrThrow_ShouldThrowNotFoundException_WhenUserNotFound() {
        testWhenEntityNotFound(usersRepository, userId, "User", getEntityService::getUserOrThrow);
    }

    @Test
    @DisplayName("Should return Season when exists")
    void getSeasonOrThrow_ShouldReturnSeason_WhenExists() {
        testEntityGetOrThrow(seasonsRepository, seasonId, new Season(), getEntityService::getSeasonOrThrow);
    }

    @Test
    @DisplayName("Should throw NotFoundException when season not found")
    void getSeasonOrThrow_ShouldThrowNotFoundException_WhenSeasonNotFound() {
        testWhenEntityNotFound(seasonsRepository, seasonId, "Season", getEntityService::getSeasonOrThrow);
    }

    @Test
    @DisplayName("Should return League when exists")
    void getLeagueOrThrow_ShouldReturnLeague_WhenExists() {
        testEntityGetOrThrow(leaguesRepository, leagueId, new League(), getEntityService::getLeagueOrThrow);
    }

    @Test
    @DisplayName("Should throw NotFoundException when league not found")
    void getLeagueOrThrow_ShouldThrowNotFoundException_WhenLeagueNotFound() {
        testWhenEntityNotFound(leaguesRepository, leagueId, "League", getEntityService::getLeagueOrThrow);
    }

    @Test
    @DisplayName("Should return Team when exists")
    void getTeamOrThrow_ShouldReturnTeam_WhenExists() {
        testEntityGetOrThrow(teamsRepository, teamId, new Team(), getEntityService::getTeamOrThrow);
    }

    @Test
    @DisplayName("Should throw NotFoundException when team not found")
    void getTeamOrThrow_ShouldThrowNotFoundException_WhenTeamNotFound() {
        testWhenEntityNotFound(teamsRepository, teamId, "Team", getEntityService::getTeamOrThrow);
    }

    @Test
    @DisplayName("Should return Bet when exists")
    void getBetOrThrow_ShouldReturnBet_WhenExists() {
        testEntityGetOrThrow(betsRepository, betId, new Bet(), getEntityService::getBetOrThrow);
    }

    @Test
    @DisplayName("Should throw NotFoundException when bet not found")
    void getBetOrThrow_ShouldThrowNotFoundException_WhenBetNotFound() {
        testWhenEntityNotFound(betsRepository, betId, "Bet", getEntityService::getBetOrThrow);
    }

    @Test
    @DisplayName("Should return CalendarNode when exists")
    void getCalendarNodeOrThrow_ShouldReturnCalendarNode_WhenExists() {
        testEntityGetOrThrow(calendarsRepository, calendarNodeId, new CalendarNode(), getEntityService::getCalendarNodeOrThrow);
    }

    @Test
    @DisplayName("Should throw NotFoundException when calendar node not found")
    void getCalendarNodeOrThrow_ShouldThrowNotFoundException_WhenCalendarNodeNotFound() {
        testWhenEntityNotFound(calendarsRepository, calendarNodeId, "CalendarNode", getEntityService::getCalendarNodeOrThrow);
    }

    // ------------------------------------------------------------------------------------------------------ //

    private <T> void testEntityGetOrThrow(MongoRepository<T, String> repository, String id, T expectedEntity, ThrowingFunction<String, T> serviceCall) {
        // given
        when(repository.findById(id)).thenReturn(Optional.of(expectedEntity));

        // when
        T actualEntity = serviceCall.apply(id);

        // then
        assertNotNull(actualEntity);
        assertEquals(expectedEntity, actualEntity);
        verify(repository, times(1)).findById(id);
    }

    private <T> void testWhenEntityNotFound(MongoRepository<T, String> repository, String id, String entityName, ThrowingFunction<String, T> serviceCall) {
        // given
        String expectedNotFoundMessage = "Expected NotFoundException with formatted message";
        when(repository.findById(id)).thenReturn(Optional.empty());

        // when + then
        NotFoundException exception = assertThrows(NotFoundException.class, () -> serviceCall.apply(id));
        assertEquals(NotFoundException.formatMessage(entityName, id), exception.getMessage(), expectedNotFoundMessage);
        verify(repository, times(1)).findById(id);
    }

    @FunctionalInterface
    interface ThrowingFunction<T, R> {
        R apply(T t) throws NotFoundException;
    }

    // ------------------------------------------------------------------------------------------------------ //

    @Test
    @DisplayName("Should return LeagueMatchdayNode when leagueId matches")
    void getLeagueMatchdayNodeOrThrow_ShouldReturnLeagueMatchdayNode_WhenLeagueIdMatches() {
        // given
        String matchday = "matchday";
        LeagueMatchdayNode expectedNode = LeagueMatchdayNode.builder()
                .leagueId(leagueId)
                .matchDay(matchday)
                .build();

        CalendarNode calendarNode = CalendarNode.builder()
                .leagueMatchdayNodes(List.of(expectedNode))
                .build();


        // when
        LeagueMatchdayNode actualNode = getEntityService.getLeagueMatchdayNodeOrThrow(calendarNode, leagueId, matchday);

        // then
        assertNotNull(actualNode);
        assertEquals(expectedNode, actualNode);
    }

    @ParameterizedTest
    @MethodSource("provideInvalidCalendarNodeInputs")
    @DisplayName("Should throw BadRequestException for various invalid inputs")
    void getLeagueMatchdayNodeOrThrow_ShouldThrowBadRequestException_ForInvalidInputs(CalendarNode calendarNode, String leagueId, String matchday) {
        // when + then
        BadRequestException exception = assertThrows(BadRequestException.class, () ->
                getEntityService.getLeagueMatchdayNodeOrThrow(calendarNode, leagueId, matchday));
        assertEquals("leagueNotFoundInCalendarNode", exception.getMessage());
    }

    private static Stream<Arguments> provideInvalidCalendarNodeInputs() {
        String leagueId = "leagueId";
        String matchday = "matchday";
        return Stream.of(
                arguments(CalendarNode.builder().leagueMatchdayNodes(List.of(LeagueMatchdayNode.builder().leagueId("leagueId-not-match").build()))
                        .build(), leagueId, matchday),

                arguments(CalendarNode.builder().leagueMatchdayNodes(List.of())
                        .build(), leagueId, matchday),

                arguments(CalendarNode.builder().leagueMatchdayNodes(List.of(LeagueMatchdayNode.builder().leagueId(leagueId).build()))
                        .build(), null, matchday),

                arguments(CalendarNode.builder().leagueMatchdayNodes(List.of(LeagueMatchdayNode.builder().leagueId(leagueId).build()))
                        .build(), leagueId, null)
        );
    }

    // ------------------------------------------------------------------------------------------------------ //

    @Test
    @DisplayName("Should return PlayerStats when it exists")
    void getPlayerStatsOrNull_ShouldReturnPlayerStats_WhenExists() {
        // given
        User user = new User();
        PlayerStats expectedPlayerStats = PlayerStats.builder()
                .seasonId(seasonId)
                .leagueId(leagueId)
                .user(user)
                .build();

        when(playerStatsRepository.findBySeasonIdAndLeagueIdAndUser(seasonId, leagueId, user))
                .thenReturn(Optional.of(expectedPlayerStats));

        // when
        PlayerStats actualPlayerStats = getEntityService.getPlayerStatsOrNull(seasonId, leagueId, user);

        // then
        assertNotNull(actualPlayerStats);
        assertEquals(expectedPlayerStats, actualPlayerStats);
        verify(playerStatsRepository, times(1)).findBySeasonIdAndLeagueIdAndUser(seasonId, leagueId, user);
    }

    @Test
    @DisplayName("Should return null for invalid parameters")
    void getPlayerStatsOrNull_ShouldReturnNull_WhenParametersAreInvalid() {
        // given
        User user = new User();
        when(playerStatsRepository.findBySeasonIdAndLeagueIdAndUser(seasonId, leagueId, user))
                .thenReturn(Optional.empty());

        // when
        PlayerStats actualResult = getEntityService.getPlayerStatsOrNull(seasonId, leagueId, user);

        // then
        assertNull(actualResult);
        verify(playerStatsRepository, times(1)).findBySeasonIdAndLeagueIdAndUser(seasonId, leagueId, user);
    }

    // ------------------------------------------------------------------------------------------------------ //

    @Test
    @DisplayName("Should return PlayerStatsByTeams when exists")
    void getPlayerStatsByTeamsOrThrow_ShouldReturnPlayerStatsByTeams_WhenExists() {
        // given
        PlayerStatsByTeams expectedPlayerStatsByTeams = new PlayerStatsByTeams();

        when(playerStatsByTeamsRepository.findBySeasonIdAndLeagueIdAndUserId(seasonId, leagueId, userId))
                .thenReturn(Optional.of(expectedPlayerStatsByTeams));

        // when
        PlayerStatsByTeams actualPlayerStatsByTeams = getEntityService.getPlayerStatsByTeamsOrThrow(seasonId, leagueId, userId);

        // then
        assertNotNull(actualPlayerStatsByTeams);
        assertEquals(expectedPlayerStatsByTeams, actualPlayerStatsByTeams);
        verify(playerStatsByTeamsRepository, times(1)).findBySeasonIdAndLeagueIdAndUserId(seasonId, leagueId, userId);
    }

    @Test
    @DisplayName("Should throw BadRequestException when PlayerStatsByTeams not found")
    void getPlayerStatsByTeamsOrThrow_ShouldThrowBadRequestException_WhenPlayerStatsByTeamsNotFound() {
        // given
        when(playerStatsByTeamsRepository.findBySeasonIdAndLeagueIdAndUserId(seasonId, leagueId, userId))
                .thenReturn(Optional.empty());

        // when + then
        BadRequestException exception = assertThrows(BadRequestException.class, () ->
                getEntityService.getPlayerStatsByTeamsOrThrow(seasonId, leagueId, userId));

        assertEquals("noPlayerStatsByTeamsInLeague", exception.getMessage());
        verify(playerStatsByTeamsRepository, times(1)).findBySeasonIdAndLeagueIdAndUserId(seasonId, leagueId, userId);
    }

    // ------------------------------------------------------------------------------------------------------ //

    @Test
    @DisplayName("Should return list of CalendarNodes when exists")
    void getListOfCalendarNodesBySeasonOrThrow_ShouldReturnListOfCalendarNodes_WhenExists() {
        // given
        List<CalendarNode> expectedCalendarNodes = List.of(new CalendarNode());

        when(calendarsRepository.findBySeasonId(seasonId)).thenReturn(Optional.of(expectedCalendarNodes));

        // when
        List<CalendarNode> actualCalendarNodes = getEntityService.getListOfCalendarNodesBySeasonOrThrow(seasonId);

        // then
        assertNotNull(actualCalendarNodes);
        assertEquals(expectedCalendarNodes, actualCalendarNodes);
        verify(calendarsRepository, times(1)).findBySeasonId(seasonId);
    }

    @Test
    @DisplayName("Should throw BadRequestException when no CalendarNodes found")
    void getListOfCalendarNodesBySeasonOrThrow_ShouldThrowBadRequestException_WhenCalendarNodesNotFound() {
        // given
        when(calendarsRepository.findBySeasonId(seasonId)).thenReturn(Optional.empty());

        // when + then
        BadRequestException exception = assertThrows(BadRequestException.class, () ->
                getEntityService.getListOfCalendarNodesBySeasonOrThrow(seasonId));

        assertEquals("noCalendarNodesBySeason", exception.getMessage());
        verify(calendarsRepository, times(1)).findBySeasonId(seasonId);
    }

    // ------------------------------------------------------------------------------------------------------ //

    @Test
    @DisplayName("Should return list of CalendarNodes which has bets")
    void getListOfCalendarNodesWithBetsBySeasonOrThrow_ShouldReturnListOfCalendarNodeWithBets_WhenExists() {
        // given
        List<CalendarNode> expectedCalendarNodes = List.of(new CalendarNode());
        when(calendarsRepository.findBySeasonIdAndHasBets(seasonId, true))
                .thenReturn(Optional.of(expectedCalendarNodes));

        // when
        List<CalendarNode> actualCalendarNodes = getEntityService.getListOfCalendarNodesWithBetsBySeasonOrThrow(seasonId);

        // then
        assertNotNull(actualCalendarNodes);
        assertEquals(expectedCalendarNodes, actualCalendarNodes);
        verify(calendarsRepository, times(1)).findBySeasonIdAndHasBets(seasonId, true);
    }

    @Test
    @DisplayName("Should throw BadRequestException when no CalendarNodes with bets found")
    void getListOfCalendarNodesWithBetsBySeasonOrThrow_ShouldThrowBadRequestException_WhenCalendarNodesWithBetsNotFound() {
        // given
        when(calendarsRepository.findBySeasonIdAndHasBets(seasonId, true)).thenReturn(Optional.empty());

        // when + then
        BadRequestException exception = assertThrows(BadRequestException.class, () ->
                getEntityService.getListOfCalendarNodesWithBetsBySeasonOrThrow(seasonId));

        assertEquals("noCalendarNodesBySeason", exception.getMessage());
        verify(calendarsRepository, times(1)).findBySeasonIdAndHasBets(seasonId, true);
    }

    // ------------------------------------------------------------------------------------------------------ //

    @Test
    @DisplayName("Should return list of finished CalendarNodes when exists")
    void getListOfCalendarNodesIsFinishedOrThrow_ShouldReturnListOfFinishedCalendarNodes_WhenExists() {
        // given
        List<CalendarNode> expectedCalendarNodes = List.of(new CalendarNode());
        when(calendarsRepository.findBySeasonIdAndIsFinished(seasonId, true))
                .thenReturn(Optional.of(expectedCalendarNodes));

        // when
        List<CalendarNode> actualCalendarNodes = getEntityService.getListOfCalendarNodesIsFinishedOrThrow(seasonId);

        // then
        assertNotNull(actualCalendarNodes);
        assertEquals(expectedCalendarNodes, actualCalendarNodes);
        verify(calendarsRepository, times(1)).findBySeasonIdAndIsFinished(seasonId, true);
    }

    @Test
    @DisplayName("Should throw BadRequestException when no finished CalendarNodes found")
    void getListOfCalendarNodesIsFinishedOrThrow_ShouldThrowBadRequestException_WhenNotFound() {
        // given
        when(calendarsRepository.findBySeasonIdAndIsFinished(seasonId, true)).thenReturn(Optional.empty());

        // when + then
        BadRequestException exception = assertThrows(BadRequestException.class, () ->
                getEntityService.getListOfCalendarNodesIsFinishedOrThrow(seasonId));

        assertEquals("noCalendarNodesBySeason", exception.getMessage());
        verify(calendarsRepository, times(1)).findBySeasonIdAndIsFinished(seasonId, true);
    }

}
