package net.friendly_bets.utils;

import lombok.RequiredArgsConstructor;
import net.friendly_bets.exceptions.NotFoundException;
import net.friendly_bets.models.*;
import net.friendly_bets.repositories.*;

@RequiredArgsConstructor
public class GetEntityOrThrow {

    public static User getUserOrThrow(UsersRepository usersRepository, String userId) {
        return usersRepository.findById(userId).orElseThrow(
                () -> new NotFoundException("Пользователь", userId));
    }

    public static Season getSeasonOrThrow(SeasonsRepository seasonsRepository, String seasonId) {
        return seasonsRepository.findById(seasonId).orElseThrow(
                () -> new NotFoundException("Сезон", seasonId));
    }

    public static League getLeagueOrThrow(LeaguesRepository leaguesRepository, String leagueId) {
        return leaguesRepository.findById(leagueId).orElseThrow(
                () -> new NotFoundException("Лига", leagueId));
    }

    public static Team getTeamOrThrow(TeamsRepository teamsRepository, String teamId) {
        return teamsRepository.findById(teamId).orElseThrow(
                () -> new NotFoundException("Команда", teamId));
    }

    public static Bet getBetOrThrow(BetsRepository betsRepository, String betId) {
        return betsRepository.findById(betId).orElseThrow(
                () -> new NotFoundException("Ставка", betId));
    }

    public static PlayerStats getDefaultPlayerStats(String seasonId, String leagueId, User user) {
        return PlayerStats.builder()
                .seasonId(seasonId)
                .leagueId(leagueId)
                .user(user)
                .totalBets(0)
                .betCount(0)
                .wonBetCount(0)
                .returnedBetCount(0)
                .lostBetCount(0)
                .emptyBetCount(0)
                .winRate(0.0)
                .averageOdds(0.0)
                .averageWonBetOdds(0.0)
                .actualBalance(0.0)
                .sumOfOdds(0.0)
                .sumOfWonOdds(0.0)
                .build();
    }
}
