package net.friendly_bets.utils;

import net.friendly_bets.exceptions.BadRequestException;
import net.friendly_bets.exceptions.ConflictException;
import net.friendly_bets.models.*;
import net.friendly_bets.repositories.BetsRepository;
import net.friendly_bets.repositories.CalendarsRepository;

import java.time.LocalDate;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

import static net.friendly_bets.utils.GetEntityOrThrow.getListOfCalendarNodesBySeasonOrThrow;

public class BetValuesUtils {

    private static final String SCORE_PATTERN = "^\\d+:\\d+ \\(\\d+:\\d+\\)$";
    private static final String SCORE_OT_PATTERN = "^\\d+:\\d+ \\(\\d+:\\d+\\) \\[доп\\.\\d+:\\d+\\]$";
    private static final String SCORE_PENALTY_PATTERN = "^\\d+:\\d+ \\(\\d+:\\d+\\) \\[доп\\.\\d+:\\d+, пен\\.\\d+:\\d+\\]$";

    public static void checkGameResult(String score) {
        if (!Pattern.matches(SCORE_PATTERN, score) && !Pattern.matches(SCORE_OT_PATTERN, score) && !Pattern.matches(SCORE_PENALTY_PATTERN, score)) {
            throw new BadRequestException("invalidGamescore");
        }
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

    public static void setCurrentMatchDay(BetsRepository betsRepository, Season season, League league) {
        if (season.getPlayers().size() != 0 && season.getBetCountPerMatchDay() != 0) {
            int totalBets = betsRepository.countBetsByLeagueAndBetStatusNot(league, Bet.BetStatus.DELETED);
            int currentMatchDay = totalBets / (season.getPlayers().size() * season.getBetCountPerMatchDay()) + 1;
            league.setCurrentMatchDay(String.valueOf(currentMatchDay));
        }
    }

    public static void setBalanceChange(Bet bet, Bet.BetStatus betStatus, Integer betSize, Double betOdds) {
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

    public static void datesRangeValidation(LocalDate startDate, LocalDate endDate) {
        if (startDate.isAfter(endDate)) {
            throw new ConflictException("startDateMustBeBeforeOrEqualToEndDate");
        }
    }

    public static void leagueMatchdaysValidation(CalendarsRepository calendarsRepository, List<LeagueMatchdayNode> matchdayNodes, String seasonId) {
        Set<String> uniqueCombinations = new HashSet<>();
        List<CalendarNode> calendarNodes = getListOfCalendarNodesBySeasonOrThrow(calendarsRepository, seasonId);

        for (CalendarNode calendarNode : calendarNodes) {
            List<LeagueMatchdayNode> leagueMatchdayNodes = calendarNode.getLeagueMatchdayNodes();
            for (LeagueMatchdayNode leagueMatchdayNode : leagueMatchdayNodes) {
                String combination;
                if (leagueMatchdayNode.getMatchDay().equals("final")) {
                    combination = leagueMatchdayNode.getLeagueId() + leagueMatchdayNode.getMatchDay();
                } else {
                    combination = leagueMatchdayNode.getLeagueId() + leagueMatchdayNode.getMatchDay() + leagueMatchdayNode.getPlayoffRound();
                }
                uniqueCombinations.add(combination);
            }
        }

        for (LeagueMatchdayNode node : matchdayNodes) {
            String combination;
            if (node.getMatchDay().equals("final")) {
                combination = node.getLeagueId() + node.getMatchDay();
            } else {
                combination = node.getLeagueId() + node.getMatchDay() + node.getPlayoffRound();
            }

            if (!uniqueCombinations.add(combination)) {
                if (node.getIsPlayoff() && !node.getMatchDay().equals("final")) {
                    throw new ConflictException("Выбранная лига с указанным туром уже добавлена в календарь: " + node.getLeagueCode() + " - " + node.getMatchDay() + " [" + node.getPlayoffRound() + "]");
                } else {
                    throw new ConflictException("Выбранная лига с указанным туром уже добавлена в календарь: " + node.getLeagueCode() + " - " + node.getMatchDay());
                }
            }
        }
    }
}
