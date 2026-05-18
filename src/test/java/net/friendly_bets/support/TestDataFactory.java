package net.friendly_bets.support;

import lombok.RequiredArgsConstructor;
import net.friendly_bets.dto.BetDto;
import net.friendly_bets.dto.NewBetDto;
import net.friendly_bets.models.*;
import net.friendly_bets.models.enums.BetTitleCode;
import net.friendly_bets.repositories.*;
import net.friendly_bets.services.BetsService;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static net.friendly_bets.utils.BetUtils.createNewOpenedBet;

/**
 * Фабрика тестовых данных для интеграционных тестов (пишет в MongoDB через репозитории).
 */
@Component
@RequiredArgsConstructor
public class TestDataFactory {

    private static final AtomicInteger SEQUENCE = new AtomicInteger();

    private final UsersRepository usersRepository;
    private final TeamsRepository teamsRepository;
    private final LeaguesRepository leaguesRepository;
    private final SeasonsRepository seasonsRepository;
    private final CalendarsRepository calendarsRepository;
    private final BetsRepository betsRepository;
    private final BetsService betsService;

    // ------------------------------------------------------------------------------------------------------ //

    public User createUser(User.Role role) {
        String suffix = nextSuffix();
        User user = User.builder()
                .createdAt(LocalDateTime.now())
                .email("test-" + suffix + "@friendly-bets.local")
                .emailIsConfirmed(true)
                .hashPassword("test-password-hash")
                .role(role)
                .username("user_" + suffix)
                .language("ru")
                .build();
        return usersRepository.save(user);
    }

    public User createPlayer() {
        return createUser(User.Role.USER);
    }

    public User createModerator() {
        return createUser(User.Role.MODERATOR);
    }

    // ------------------------------------------------------------------------------------------------------ //

    public Team createTeam(String title) {
        String suffix = nextSuffix();
        Team team = Team.builder()
                .createdAt(LocalDateTime.now())
                .title(title + " " + suffix)
                .country("Testland")
                .build();
        return teamsRepository.save(team);
    }

    public Team createHomeTeam() {
        return createTeam("Home");
    }

    public Team createAwayTeam() {
        return createTeam("Away");
    }

    // ------------------------------------------------------------------------------------------------------ //

    public League createLeague(League.LeagueCode leagueCode) {
        League league = League.builder()
                .createdAt(LocalDateTime.now())
                .leagueCode(leagueCode)
                .name(leagueCode.name() + " Test")
                .currentMatchDay("1")
                .teams(new ArrayList<>())
                .build();
        return leaguesRepository.save(league);
    }

    public League createLeague() {
        return createLeague(League.LeagueCode.EPL);
    }

    public League addTeamToLeague(League league, Team team) {
        league.getTeams().add(team);
        return leaguesRepository.save(league);
    }

    // ------------------------------------------------------------------------------------------------------ //

    public Season createSeason() {
        return createSeason("Test Season " + nextSuffix(), 1, Season.Status.ACTIVE);
    }

    public Season createSeason(String title, int betCountPerMatchDay, Season.Status status) {
        Season season = Season.builder()
                .createdAt(LocalDateTime.now())
                .title(title)
                .betCountPerMatchDay(betCountPerMatchDay)
                .status(status)
                .players(new ArrayList<>())
                .leagues(new ArrayList<>())
                .build();
        return seasonsRepository.save(season);
    }

    public Season addPlayerToSeason(Season season, User player) {
        season.getPlayers().add(player);
        return seasonsRepository.save(season);
    }

    public Season addLeagueToSeason(Season season, League league) {
        season.getLeagues().add(league);
        return seasonsRepository.save(season);
    }

    // ------------------------------------------------------------------------------------------------------ //

    public CalendarNode createCalendarNode(String seasonId, League league, String matchDay, int betCountLimit) {
        LeagueMatchdayNode matchdayNode = LeagueMatchdayNode.builder()
                .leagueId(league.getId())
                .leagueCode(league.getLeagueCode())
                .matchDay(matchDay)
                .betCountLimit(betCountLimit)
                .bets(new ArrayList<>())
                .build();

        CalendarNode calendarNode = CalendarNode.builder()
                .createdAt(LocalDateTime.now())
                .seasonId(seasonId)
                .startDate(LocalDate.now())
                .endDate(LocalDate.now().plusDays(7))
                .leagueMatchdayNodes(new ArrayList<>(List.of(matchdayNode)))
                .hasBets(false)
                .isFinished(false)
                .gameweekStats(new ArrayList<>())
                .build();

        return calendarsRepository.save(calendarNode);
    }

    public CalendarNode createCalendarNode(String seasonId, League league) {
        return createCalendarNode(seasonId, league, "1", 1);
    }

    // ------------------------------------------------------------------------------------------------------ //

    public BetTitle createDefaultBetTitle() {
        return BetTitle.builder()
                .code(BetTitleCode.HOME_WIN.getCode())
                .label(BetTitleCode.HOME_WIN.getLabel())
                .isNot(false)
                .build();
    }

    public Bet createOpenedBet(User moderator,
                               User player,
                               Season season,
                               League league,
                               Team homeTeam,
                               Team awayTeam,
                               CalendarNode calendarNode,
                               String matchDay) {
        NewBetDto newBetDto = NewBetDto.builder()
                .userId(player.getId())
                .seasonId(season.getId())
                .leagueId(league.getId())
                .matchDay(matchDay)
                .homeTeamId(homeTeam.getId())
                .awayTeamId(awayTeam.getId())
                .betTitle(createDefaultBetTitle())
                .betOdds(2.0)
                .betSize(10)
                .calendarNodeId(calendarNode.getId())
                .build();

        Bet bet = createNewOpenedBet(newBetDto, moderator, player, season, league, homeTeam, awayTeam);
        bet = betsRepository.save(bet);
        addBetToCalendarNode(calendarNode.getId(), league.getId(), matchDay, bet);
        return bet;
    }

    public Bet createWonBet(User moderator,
                            User player,
                            Season season,
                            League league,
                            Team homeTeam,
                            Team awayTeam,
                            CalendarNode calendarNode,
                            String matchDay,
                            double betOdds,
                            int betSize) {
        GameScore gameScore = GameScore.builder()
                .fullTime("2:1")
                .firstTime("1:0")
                .build();

        Bet bet = Bet.builder()
                .createdAt(LocalDateTime.now())
                .createdBy(moderator)
                .user(player)
                .season(season)
                .league(league)
                .matchDay(matchDay)
                .homeTeam(homeTeam)
                .awayTeam(awayTeam)
                .betTitle(createDefaultBetTitle())
                .betOdds(betOdds)
                .betSize(betSize)
                .betResultAddedAt(LocalDateTime.now())
                .betResultAddedBy(moderator)
                .gameScore(gameScore)
                .betStatus(Bet.BetStatus.WON)
                .balanceChange(betOdds * betSize - betSize)
                .calendarNodeId(calendarNode.getId())
                .build();

        bet = betsRepository.save(bet);
        addBetToCalendarNode(calendarNode.getId(), league.getId(), matchDay, bet);
        return bet;
    }

    // ------------------------------------------------------------------------------------------------------ //

    /**
     * Сезон с одним игроком, лигой EPL, двумя командами и пустым календарным узлом (без ставок).
     */
    public TestFixture createMinimalSeasonSetup() {
        User moderator = createModerator();
        User player = createPlayer();
        Team homeTeam = createHomeTeam();
        Team awayTeam = createAwayTeam();
        League league = createLeague();
        league = addTeamToLeague(league, homeTeam);
        league = addTeamToLeague(league, awayTeam);

        Season season = createSeason();
        season = addPlayerToSeason(season, player);
        season = addLeagueToSeason(season, league);

        String matchDay = "1";
        CalendarNode calendarNode = createCalendarNode(season.getId(), league, matchDay, 1);

        return TestFixture.builder()
                .moderator(moderator)
                .player(player)
                .season(season)
                .league(league)
                .homeTeam(homeTeam)
                .awayTeam(awayTeam)
                .calendarNode(calendarNode)
                .matchDay(matchDay)
                .build();
    }

    /**
     * {@link #createMinimalSeasonSetup()} + открытая ставка через {@link BetsService} (со статистикой).
     */
    public TestFixture createSeasonWithOpenedBet() {
        TestFixture fixture = createMinimalSeasonSetup();
        Bet bet = addOpenedBetViaService(fixture);
        return fixture.toBuilder().bet(bet).build();
    }

    /**
     * {@link #createSeasonWithOpenedBet()} + результат WON через {@link BetsService}.
     */
    public TestFixture createSeasonWithWonBet() {
        TestFixture fixture = createSeasonWithOpenedBet();
        Bet bet = setBetResultViaService(fixture, Bet.BetStatus.WON, defaultWinGameScore());
        return fixture.toBuilder().bet(bet).build();
    }

    public NewBetDto buildNewOpenedBetDto(TestFixture fixture) {
        return NewBetDto.builder()
                .userId(fixture.getPlayer().getId())
                .seasonId(fixture.getSeason().getId())
                .leagueId(fixture.getLeague().getId())
                .matchDay(fixture.getMatchDay())
                .homeTeamId(fixture.getHomeTeam().getId())
                .awayTeamId(fixture.getAwayTeam().getId())
                .betTitle(createDefaultBetTitle())
                .betOdds(2.0)
                .betSize(10)
                .calendarNodeId(fixture.getCalendarNode().getId())
                .build();
    }

    public Bet addOpenedBetViaService(TestFixture fixture) {
        BetDto betDto = betsService.addOpenedBet(fixture.getModerator().getId(), buildNewOpenedBetDto(fixture));
        return betsRepository.findById(betDto.getId()).orElseThrow();
    }

    public Bet setBetResultViaService(TestFixture fixture, Bet.BetStatus status, GameScore gameScore) {
        BetResult betResult = BetResult.builder()
                .gameScore(gameScore)
                .betStatus(status.name())
                .build();
        BetDto betDto = betsService.setBetResult(
                fixture.getModerator().getId(),
                fixture.getBet().getId(),
                betResult
        );
        return betsRepository.findById(betDto.getId()).orElseThrow();
    }

    public static GameScore defaultWinGameScore() {
        return GameScore.builder().fullTime("2:1").firstTime("1:0").build();
    }

    public static GameScore defaultLossGameScore() {
        return GameScore.builder().fullTime("0:1").firstTime("0:0").build();
    }

    // ------------------------------------------------------------------------------------------------------ //

    private void addBetToCalendarNode(String calendarNodeId, String leagueId, String matchDay, Bet bet) {
        CalendarNode calendarNode = calendarsRepository.findById(calendarNodeId).orElseThrow();
        LeagueMatchdayNode matchdayNode = calendarNode.getLeagueMatchdayNodes().stream()
                .filter(node -> node.getLeagueId().equals(leagueId) && node.getMatchDay().equals(matchDay))
                .findFirst()
                .orElseThrow();

        matchdayNode.getBets().add(bet);
        calendarNode.setHasBets(true);
        calendarsRepository.save(calendarNode);
    }

    private static String nextSuffix() {
        return String.valueOf(SEQUENCE.incrementAndGet());
    }
}
