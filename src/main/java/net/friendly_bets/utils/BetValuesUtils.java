package net.friendly_bets.utils;

import net.friendly_bets.exceptions.BadRequestException;
import net.friendly_bets.models.Bet;
import net.friendly_bets.models.League;
import net.friendly_bets.models.Season;
import net.friendly_bets.repositories.BetsRepository;

import java.util.regex.Pattern;

public class BetValuesUtils {

    private static final String SCORE_PATTERN = "^\\d+:\\d+ \\(\\d+:\\d+\\)$";
    private static final String SCORE_OT_PATTERN = "^\\d+:\\d+ \\(\\d+:\\d+\\) \\[доп\\.\\d+:\\d+\\]$";
    private static final String SCORE_PENALTY_PATTERN = "^\\d+:\\d+ \\(\\d+:\\d+\\) \\[доп\\.\\d+:\\d+, пен\\.\\d+:\\d+\\]$";

    public static void checkGameResult(String score) {
        if (!Pattern.matches(SCORE_PATTERN, score) && !Pattern.matches(SCORE_OT_PATTERN, score) && !Pattern.matches(SCORE_PENALTY_PATTERN, score)) {
            throw new BadRequestException("Некорректный счёт матча: " + score);
        }
    }

    public static void checkTeams(String homeTeamId, String awayTeamId) {
        if (homeTeamId.equals(awayTeamId)) {
            throw new BadRequestException("Команда хозяев не может совпадать с командой гостей");
        }
    }

    public static void checkBetOdds(Double betOdds) {
        if (betOdds.isNaN()) {
            throw new BadRequestException("Коэффициент ставки не является числом");
        }
        if (betOdds <= 1) {
            throw new BadRequestException("Коэффициент ставки не может быть меньше чем 1,01");
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
}
