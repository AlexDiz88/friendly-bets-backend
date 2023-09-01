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
}
