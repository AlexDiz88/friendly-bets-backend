package net.friendly_bets.utils;

import lombok.experimental.UtilityClass;
import net.friendly_bets.dto.BetResult;
import net.friendly_bets.dto.EditedBetDto;
import net.friendly_bets.dto.NewBet;
import net.friendly_bets.dto.NewEmptyBet;
import net.friendly_bets.exceptions.BadRequestException;
import net.friendly_bets.exceptions.ConflictException;
import net.friendly_bets.models.*;
import net.friendly_bets.repositories.BetsRepository;
import net.friendly_bets.repositories.LeaguesRepository;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static net.friendly_bets.utils.Constants.COMPLETED_BET_STATUSES;
import static net.friendly_bets.utils.Constants.WRL_STATUSES;

@UtilityClass
public class BetUtils {

    public static void checkGameResult(GameResult gameResult, Bet.BetStatus betStatus) {
        if (WRL_STATUSES.contains(betStatus)) {
            if (gameResult == null) {
                throw new BadRequestException("gameResultIsNull");
            }
            if ((gameResult.getFullTime() == null && gameResult.getFirstTime() == null)
                    || gameResult.getFullTime() == null || gameResult.getFirstTime() == null
                    || gameResult.getFullTime().isBlank() || gameResult.getFirstTime().isBlank()) {
                throw new BadRequestException("incorrectGameResult");
            }

            boolean isGameScoreValid = isGameScoreValid(gameResult);

            if (!isGameScoreValid) {
                throw new BadRequestException("incorrectGameResult");
            }
        }
    }

    private static boolean isGameScoreValid(GameResult gameResult) {
        String fullTime = gameResult.getFullTime();
        String firstTime = gameResult.getFirstTime();
        String overTime = gameResult.getOverTime();
        String penalty = gameResult.getPenalty();

        int[] fullTimeScore = parseScorePart(fullTime);
        int[] firstTimeScore = parseScorePart(firstTime);

        if (fullTimeScore == null || firstTimeScore == null) {
            return false;
        }
        // Количество голов в 1 тайме не может быть больше голов за весь матч
        if (fullTimeScore[0] < firstTimeScore[0] || fullTimeScore[1] < firstTimeScore[1]) {
            return false;
        }

        if (overTime == null && penalty == null) {
            return true;
        }
        if (overTime != null) {
            int[] overTimeScore = parseScorePart(overTime);

            if (overTimeScore == null) {
                return false;
            }
            // Если нет пенальти - в дополнительное время не может быть ничьи
            if (penalty == null) {
                return overTimeScore[0] != overTimeScore[1];
            }

            if (penalty != null) {
                // Если есть пенальти - должна быть ничья в дополнительное время
                if (overTimeScore[0] != overTimeScore[1]) {
                    return false;
                }

                int[] penaltyScore = parseScorePart(penalty);
                if (penaltyScore == null) {
                    return false;
                }
                // В серии пенальти не может быть ничьи
                if (penaltyScore[0] == penaltyScore[1]) {
                    return false;
                }

                // Разница в счете по пенальти не может быть больше 3
                int scoreDifference = Math.abs(penaltyScore[0] - penaltyScore[1]);
                return scoreDifference <= 3;
            }
        }
        return false;
    }

    private static int[] parseScorePart(String score) {
        if (score == null || score.isEmpty()) {
            return null;
        }

        String[] parts = score.split(":");
        if (parts.length != 2) {
            return null;
        }

        try {
            int home = Integer.parseInt(parts[0]);
            int away = Integer.parseInt(parts[1]);

            if (home > 50 || away > 50 || (parts[0].length() > 1 && parts[0].startsWith("0")) || (parts[1].length() > 1 && parts[1].startsWith("0"))) {
                return null;
            }

            return new int[]{home, away};
        } catch (NumberFormatException e) {
            throw new BadRequestException("incorrectGameResult");
        }
    }

    public static void validateBet(NewBet newBet) {
        checkTeams(newBet.getHomeTeamId(), newBet.getAwayTeamId());
        checkBetOdds(newBet.getBetOdds());
    }

    public static void checkTeams(String homeTeamId, String awayTeamId) {
        if (homeTeamId.equals(awayTeamId)) {
            throw new BadRequestException("homeTeamCannotBeEqualAwayTeam");
        }
    }

    public static void checkBetOdds(Double betOdds) {
        if (betOdds.isNaN()) {
            throw new BadRequestException("betCoefIsNotNumber");
        }
        if (betOdds <= 1) {
            throw new BadRequestException("betCoefCannotBeLessThan");
        }
    }

    public static void checkIfBetAlreadyExists(BetsRepository betsRepo, NewBet newBet) {
        if (betsRepo.existsBySeason_IdAndLeague_IdAndUser_IdAndMatchDayAndHomeTeam_IdAndAwayTeam_IdAndBetStatusIn(
                newBet.getSeasonId(),
                newBet.getLeagueId(),
                newBet.getUserId(),
                newBet.getMatchDay(),
                newBet.getHomeTeamId(),
                newBet.getAwayTeamId(),
                Arrays.asList(Bet.BetStatus.OPENED, Bet.BetStatus.WON, Bet.BetStatus.RETURNED, Bet.BetStatus.LOST))) {
            throw new ConflictException("betAlreadyAdded");
        }
    }

    public static void checkIfBetAlreadyEdited(BetsRepository betsRepo, EditedBetDto editedBet, Bet.BetStatus betStatus) {
        if (betsRepo.existsBySeason_IdAndLeague_IdAndUser_IdAndMatchDayAndHomeTeam_IdAndAwayTeam_IdAndBetTitleAndBetOddsAndBetSizeAndGameResultAndBetStatus(
                editedBet.getSeasonId(),
                editedBet.getLeagueId(),
                editedBet.getUserId(),
                editedBet.getMatchDay(),
                editedBet.getHomeTeamId(),
                editedBet.getAwayTeamId(),
                editedBet.getBetTitle(),
                editedBet.getBetOdds(),
                editedBet.getBetSize(),
                editedBet.getGameResult(),
                betStatus)) {
            throw new ConflictException("betAlreadyEdited");
        }
    }

    public static Bet createNewOpenedBet(NewBet newOpenedBet, User moderator, User user, Season season, League league, Team homeTeam, Team awayTeam) {
        return Bet.builder()
                .createdAt(LocalDateTime.now())
                .createdBy(moderator)
                .user(user)
                .season(season)
                .league(league)
                .matchDay(newOpenedBet.getMatchDay())
                .homeTeam(homeTeam)
                .awayTeam(awayTeam)
                .betTitle(newOpenedBet.getBetTitle())
                .betOdds(newOpenedBet.getBetOdds())
                .betSize(newOpenedBet.getBetSize())
                .betStatus(Bet.BetStatus.OPENED)
                .calendarNodeId(newOpenedBet.getCalendarNodeId())
                .build();
    }

    public static Bet createNewEmptyBet(NewEmptyBet newEmptyBet, User moderator, User user, Season season, League league) {
        return Bet.builder()
                .createdAt(LocalDateTime.now())
                .createdBy(moderator)
                .user(user)
                .season(season)
                .league(league)
                .matchDay(newEmptyBet.getMatchDay())
                .betSize(newEmptyBet.getBetSize())
                .betResultAddedAt(LocalDateTime.now())
                .betStatus(Bet.BetStatus.EMPTY)
                .balanceChange(-Double.valueOf(newEmptyBet.getBetSize()))
                .calendarNodeId(newEmptyBet.getCalendarNodeId())
                .build();
    }

    public static Bet getPreviousStateOfBet(Bet bet) {
        if (bet.getBetStatus().equals(Bet.BetStatus.EMPTY) || bet.getBetStatus().equals(Bet.BetStatus.DELETED)) {
            throw new BadRequestException("emptyAndDeletedBetsCannotBeEdited");
        }
        return Bet.builder()
                .user(bet.getUser())
                .matchDay(bet.getMatchDay())
                .homeTeam(bet.getHomeTeam())
                .awayTeam(bet.getAwayTeam())
                .betTitle(bet.getBetTitle())
                .betOdds(bet.getBetOdds())
                .betSize(bet.getBetSize())
                .betStatus(bet.getBetStatus())
                .gameResult(bet.getGameResult())
                .balanceChange(bet.getBalanceChange())
                .build();
    }

    @Transactional
    public static void updateLeagueCurrentMatchDay(LeaguesRepository leaguesRepository, BetsRepository betsRepository, Season season, League league) {
        if (season.getPlayers() == null || season.getPlayers().isEmpty()) {
            throw new BadRequestException("noPlayersInSeason");
        }
        if (season.getBetCountPerMatchDay() == null || season.getBetCountPerMatchDay() == 0) {
            throw new BadRequestException("nullOrZeroBetCountPerMatchDay");
        }
        int totalBets = betsRepository.countBetsByLeagueAndBetStatusNot(league, Bet.BetStatus.DELETED);
        int currentMatchDay = totalBets / (season.getPlayers().size() * season.getBetCountPerMatchDay()) + 1;

        if (!league.getCurrentMatchDay().equals(String.valueOf(currentMatchDay))) {
            league.setCurrentMatchDay(String.valueOf(currentMatchDay));
            leaguesRepository.save(league);
        }
    }

    public static void checkLeagueBetLimit(LeagueMatchdayNode node, String userId) {
        long count = node.getBets().stream()
                .filter(bet -> bet.getUser().getId().equals(userId))
                .count();

        if (count >= node.getBetCountLimit()) {
            throw new BadRequestException("exceededLimitBetsFromPlayer");
        }
    }

    public static void processBetResultValues(User moderator, Bet bet, BetResult betResult) {
        Bet.BetStatus betStatus = Bet.BetStatus.valueOf(betResult.getBetStatus());
        updateBalanceChange(bet, betStatus, bet.getBetSize(), bet.getBetOdds());
        bet.setBetResultAddedAt(LocalDateTime.now());
        bet.setBetResultAddedBy(moderator);
        bet.setBetStatus(betStatus);
        bet.setGameResult(betResult.getGameResult());
    }

    private static void updateBalanceChange(Bet bet, Bet.BetStatus betStatus, Integer betSize, Double betOdds) {
        if (betStatus == Bet.BetStatus.WON) {
            bet.setBalanceChange(betOdds * betSize - betSize);
        }
        if (betStatus == Bet.BetStatus.RETURNED) {
            bet.setBalanceChange(0.0);
        }
        if (betStatus == Bet.BetStatus.LOST) {
            bet.setBalanceChange(-Double.valueOf(betSize));
        }
    }

    public static void updateEditedBetValues(BetsRepository betsRepo, Bet bet, EditedBetDto editedBet,
                                             User moderator, User user, Team homeTeam, Team awayTeam) {
        updateBalanceChangeAndGameResultAndBetStatus(betsRepo, bet, editedBet);
        updateBetDetails(bet, moderator, user, editedBet, homeTeam, awayTeam);
    }

    private static void updateBalanceChangeAndGameResultAndBetStatus(BetsRepository betsRepository, Bet bet, EditedBetDto editedBet) {
        Bet.BetStatus betStatus = Bet.BetStatus.valueOf(editedBet.getBetStatus());
        checkIfBetAlreadyEdited(betsRepository, editedBet, betStatus);

        if (WRL_STATUSES.contains(bet.getBetStatus())) {
            checkGameResult(editedBet.getGameResult(), bet.getBetStatus());
            updateBalanceChange(bet, betStatus, editedBet.getBetSize(), editedBet.getBetOdds());
            bet.setGameResult(editedBet.getGameResult());
        }

        bet.setBetStatus(betStatus);
    }

    private static void updateBetDetails(Bet bet, User moderator, User user, EditedBetDto editedBet, Team homeTeam, Team awayTeam) {
        bet.setUpdatedAt(LocalDateTime.now());
        bet.setUpdatedBy(moderator);
        bet.setUser(user);
        bet.setMatchDay(editedBet.getMatchDay());
        bet.setHomeTeam(homeTeam);
        bet.setAwayTeam(awayTeam);
        bet.setBetTitle(editedBet.getBetTitle());
        bet.setBetOdds(editedBet.getBetOdds());
        bet.setBetSize(editedBet.getBetSize());
    }

    public static void updateDeletedBetValues(Bet bet, User moderator) {
        bet.setUpdatedAt(LocalDateTime.now());
        bet.setUpdatedBy(moderator);
        if (COMPLETED_BET_STATUSES.contains(bet.getBetStatus())) {
            bet.setBalanceChange(0.0);
        }
    }

    public static void datesRangeValidation(LocalDate startDate, LocalDate endDate) {
        if (startDate.isAfter(endDate)) {
            throw new ConflictException("startDateMustBeBeforeOrEqualToEndDate");
        }
    }

    public static void leagueMatchdaysValidation(List<CalendarNode> calendarNodes, List<LeagueMatchdayNode> matchdayNodes) {
        Set<String> uniqueCombinations = new HashSet<>();

        for (CalendarNode calendarNode : calendarNodes) {
            List<LeagueMatchdayNode> leagueMatchdayNodes = calendarNode.getLeagueMatchdayNodes();
            for (LeagueMatchdayNode leagueMatchdayNode : leagueMatchdayNodes) {
                String combination = leagueMatchdayNode.getLeagueId() + leagueMatchdayNode.getMatchDay();
                uniqueCombinations.add(combination);
            }
        }

        for (LeagueMatchdayNode node : matchdayNodes) {
            String combination = node.getLeagueId() + node.getMatchDay();
            if (!uniqueCombinations.add(combination)) {
                throw new ConflictException("Выбранная лига с указанным туром уже добавлена в календарь - " + node.getLeagueCode() + " " + node.getMatchDay());
            }
        }
    }
}
