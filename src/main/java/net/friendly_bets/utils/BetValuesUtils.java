package net.friendly_bets.utils;

import net.friendly_bets.exceptions.BadRequestException;
import net.friendly_bets.models.Bet;
import net.friendly_bets.models.League;
import net.friendly_bets.models.Season;

import java.util.regex.Pattern;

public class BetValuesUtils {

    private static final String SCORE_PATTERN = "^\\d+:\\d+ \\(\\d+:\\d+\\)$";

    public static void checkGameResult(String score) {
        if (!Pattern.matches(SCORE_PATTERN, score)) {
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

    public static void setCurrentMatchDay(Season season, League league) {
        if (season.getPlayers().size() != 0 && season.getBetCountPerMatchDay() != 0) {
            int totalBets = (int) league.getBets().stream().filter(b -> !b.getBetStatus().equals(Bet.BetStatus.DELETED)).count();
            int currentMatchDay = totalBets / (season.getPlayers().size() * season.getBetCountPerMatchDay()) + 1;
            league.setCurrentMatchDay(String.valueOf(currentMatchDay));
        }
    }

    public static void setBalanceChange(Bet bet, Bet.BetStatus betStatus, Integer betSize, Double betOdds) {
        if (betStatus.equals(Bet.BetStatus.WON)) {
            bet.setBalanceChange(betOdds * betSize - betSize);
        }
        if (betStatus.equals(Bet.BetStatus.RETURNED)) {
            bet.setBalanceChange(0.0);
        }
        if (betStatus.equals(Bet.BetStatus.LOST)) {
            bet.setBalanceChange(-Double.valueOf(betSize));
        }
    }
}
