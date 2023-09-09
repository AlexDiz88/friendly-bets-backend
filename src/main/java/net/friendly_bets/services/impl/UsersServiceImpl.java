package net.friendly_bets.services.impl;

import lombok.RequiredArgsConstructor;
import net.friendly_bets.dto.*;
import net.friendly_bets.exceptions.BadRequestException;
import net.friendly_bets.exceptions.ConflictException;
import net.friendly_bets.models.Bet;
import net.friendly_bets.models.League;
import net.friendly_bets.models.Season;
import net.friendly_bets.models.User;
import net.friendly_bets.repositories.SeasonsRepository;
import net.friendly_bets.repositories.UsersRepository;
import net.friendly_bets.services.UsersService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static net.friendly_bets.utils.GetEntityOrThrow.getSeasonOrThrow;
import static net.friendly_bets.utils.GetEntityOrThrow.getUserOrThrow;

@RequiredArgsConstructor
@Service
public class UsersServiceImpl implements UsersService {

    private final UsersRepository usersRepository;
    private final SeasonsRepository seasonsRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public UserDto getProfile(String currentUserId) {
        User user = getUserOrThrow(usersRepository, currentUserId);
        return UserDto.from(user);
    }

    // ------------------------------------------------------------------------------------------------------ //

    @Override
    @Transactional
    public UserDto editEmail(String currentUserId, UpdatedEmailDto updatedEmailDto) {
        User user = getUserOrThrow(usersRepository, currentUserId);
        if (user.getEmail().equals(updatedEmailDto.getNewEmail().toLowerCase())) {
            throw new ConflictException("Новый E-mail совпадает со старым");
        }
        if (usersRepository.existsByEmail(updatedEmailDto.getNewEmail().toLowerCase())) {
            throw new ConflictException("Пользователь с таким E-mail уже существует");
        }

        user.setEmail(updatedEmailDto.getNewEmail().toLowerCase());
        usersRepository.save(user);

        return UserDto.from(user);
    }

    // ------------------------------------------------------------------------------------------------------ //

    @Override
    @Transactional
    public UserDto editPassword(String currentUserId, UpdatedPasswordDto updatedPasswordDto) {
        if (updatedPasswordDto.getNewPassword().equals(updatedPasswordDto.getCurrentPassword())) {
            throw new BadRequestException("Введенные текущий и новый пароль совпадают");
        }

        User user = getUserOrThrow(usersRepository, currentUserId);

        if (!passwordEncoder.matches(updatedPasswordDto.getCurrentPassword(), user.getHashPassword())) {
            throw new BadRequestException("Введенный текущий пароль указан неверно");
        }
        if (passwordEncoder.matches(updatedPasswordDto.getNewPassword(), user.getHashPassword())) {
            throw new ConflictException("Новый пароль совпадает с текущим");
        }

        user.setHashPassword(passwordEncoder.encode(updatedPasswordDto.getNewPassword()));
        usersRepository.save(user);

        return UserDto.from(user);
    }

    // ------------------------------------------------------------------------------------------------------ //

    @Override
    @Transactional
    public UserDto editUsername(String currentUserId, UpdatedUsernameDto updatedUsernameDto) {
        User user = getUserOrThrow(usersRepository, currentUserId);
        if (user.getUsername() != null && user.getUsername().equals(updatedUsernameDto.getNewUsername())) {
            throw new BadRequestException("Новое имя совпадает со старым");
        }
        if (usersRepository.existsByUsername(updatedUsernameDto.getNewUsername())) {
            throw new ConflictException("Пользователь с таким именем уже существует. Выберите себе другое имя");
        }

        user.setUsername(updatedUsernameDto.getNewUsername());
        usersRepository.save(user);

        return UserDto.from(user);
    }

    // ------------------------------------------------------------------------------------------------------ //

//    @Override
//    public PlayersStatsPage getPlayersStatsBySeason(String seasonId) {
//        Season season = seasonsRepository.findById(seasonId).orElseThrow(
//                () -> new NotFoundException("Сезон с ID <" + seasonId + "> не найден")
//        );
//        List<League> leagues = season.getLeagues();
//        Map<String, PlayerStatsDto> playersStatsMap = new HashMap<>();
//
//        for (League league : leagues) {
//            List<Bet> bets = league.getBets();
//            for (Bet bet : bets) {
//                if (bet.getBetStatus().equals(Bet.BetStatus.DELETED)) {
//                    continue;
//                }
//
//                String username = bet.getUser().getUsername();
//
//                PlayerStatsDto playerStats = playersStatsMap.getOrDefault(
//                        username,
//                        PlayerStatsDto.builder()
//                                .avatar(bet.getUser().getAvatar())
//                                .username(username)
//                                .totalBets(0)
//                                .betCount(0)
//                                .wonBetCount(0)
//                                .returnedBetCount(0)
//                                .lostBetCount(0)
//                                .emptyBetCount(0)
//                                .winRate(0.0)
//                                .averageOdds(0.0)
//                                .averageWonBetOdds(0.0)
//                                .actualBalance(0.0)
//                                .sumOfOdds(0.0)
//                                .sumOfWonOdds(0.0)
//                                .build());
//
//                if (bet.getBetStatus().equals(Bet.BetStatus.OPENED)) {
//                    playerStats.setTotalBets(playerStats.getTotalBets() + 1);
//                    continue;
//                }
//
//                Double balanceChange = 0.0;
//                if (bet.getBalanceChange() != null) {
//                    balanceChange = bet.getBalanceChange();
//                }
//                Double odds = 0.0;
//                if (bet.getBetOdds() != null) {
//                    odds = bet.getBetOdds();
//                }
//                playerStats.setActualBalance(playerStats.getActualBalance() + balanceChange);
//                playerStats.setTotalBets(playerStats.getTotalBets() + 1);
//                playerStats.setBetCount(playerStats.getBetCount() + 1);
//                playerStats.setSumOfOdds(playerStats.getSumOfOdds() + odds);
//
//                if (bet.getBetStatus().equals(Bet.BetStatus.WON)) {
//                    playerStats.setWonBetCount(playerStats.getWonBetCount() + 1);
//                    playerStats.setSumOfWonOdds(playerStats.getSumOfWonOdds() + odds);
//                }
//                if (bet.getBetStatus().equals(Bet.BetStatus.RETURNED)) {
//                    playerStats.setReturnedBetCount(playerStats.getReturnedBetCount() + 1);
//                }
//                if (bet.getBetStatus().equals(Bet.BetStatus.LOST)) {
//                    playerStats.setLostBetCount(playerStats.getLostBetCount() + 1);
//                }
//                if (bet.getBetStatus().equals(Bet.BetStatus.EMPTY)) {
//                    playerStats.setEmptyBetCount(playerStats.getEmptyBetCount() + 1);
//                }
//
//                playersStatsMap.put(username, playerStats);
//            }
//        }
//
//        for (PlayerStatsDto playerStats : playersStatsMap.values()) {
//            double averageOdds = playerStats.getAverageOdds();
//            playerStats.setAverageOdds(averageOdds);
//            double averageWonOdds = playerStats.getAverageWonOdds();
//            playerStats.setAverageWonBetOdds(averageWonOdds);
//            if (playerStats.getBetCount() - playerStats.getReturnedBetCount() - playerStats.getEmptyBetCount() == 0) {
//                playerStats.setWinRate(0.0);
//            } else {
//                double winRate = 100.0 * playerStats.getWonBetCount() / (playerStats.getBetCount() - playerStats.getReturnedBetCount() - playerStats.getEmptyBetCount());
//                playerStats.setWinRate(winRate);
//            }
//        }
//
//        List<PlayerStatsDto> playerStatsList = new ArrayList<>(playersStatsMap.values());
//        return new PlayersStatsPage(playerStatsList);
//    }

    // ------------------------------------------------------------------------------------------------------ //

    // TODO: использовать этот вариант как альтернативный по лигам и игрокам отдельно
    //@Override
    public PlayersStatsPage getPlayersStatsByLeaguesSeparately(String seasonId) {
        Season season = getSeasonOrThrow(seasonsRepository, seasonId);
        List<League> leagues = season.getLeagues();
        List<PlayerStatsDto> playersStatsByLeagues = new ArrayList<>();

        for (League league : leagues) {
            List<PlayerStatsDto> playersStatsByLeague = getPlayersStatsByLeague(league);
            playersStatsByLeagues.addAll(playersStatsByLeague);
        }
        return new PlayersStatsPage(playersStatsByLeagues);
    }

    // ------------------------------------------------------------------------------------------------------ //

    public PlayersStatsPage getPlayersStatsBySeason(String seasonId) {
        Season season = getSeasonOrThrow(seasonsRepository, seasonId);
        List<League> leagues = season.getLeagues();
        Map<String, PlayerStatsDto> combinedPlayerStatsMap = new HashMap<>();
        for (League league : leagues) {
            if (league == null || league.getBets() == null || league.getBets().isEmpty()) {
                continue;
            }

            List<PlayerStatsDto> playersStatsByLeague = getPlayersStatsByLeague(league);
            for (PlayerStatsDto playerStats : playersStatsByLeague) {
                String username = playerStats.getUsername();
                combinedPlayerStatsMap.putIfAbsent(username, getDefaultPlayerStatsDto(playerStats.getAvatar(), username));

                PlayerStatsDto combinedStats = combinedPlayerStatsMap.get(username);
                combinedStats.setTotalBets(combinedStats.getTotalBets() + playerStats.getTotalBets());
                combinedStats.setBetCount(combinedStats.getBetCount() + playerStats.getBetCount());
                combinedStats.setWonBetCount(combinedStats.getWonBetCount() + playerStats.getWonBetCount());
                combinedStats.setReturnedBetCount(combinedStats.getReturnedBetCount() + playerStats.getReturnedBetCount());
                combinedStats.setLostBetCount(combinedStats.getLostBetCount() + playerStats.getLostBetCount());
                combinedStats.setEmptyBetCount(combinedStats.getEmptyBetCount() + playerStats.getEmptyBetCount());
                combinedStats.setActualBalance(combinedStats.getActualBalance() + playerStats.getActualBalance());
                combinedStats.setSumOfOdds(combinedStats.getSumOfOdds() + playerStats.getSumOfOdds());
                combinedStats.setSumOfWonOdds(combinedStats.getSumOfWonOdds() + playerStats.getSumOfWonOdds());
            }
        }

        List<PlayerStatsDto> combinedPlayerStatsList = new ArrayList<>(combinedPlayerStatsMap.values());
        for (PlayerStatsDto combinedStats : combinedPlayerStatsList) {
            combinedStats.setWinRate(combinedStats.calculateWinRate());
            combinedStats.setAverageOdds(combinedStats.getAverageOdds());
            combinedStats.setAverageWonBetOdds(combinedStats.getAverageWonBetOdds());
        }

        return new PlayersStatsPage(combinedPlayerStatsList);
    }

    // ------------------------------------------------------------------------------------------------------ //

    public PlayersStatsByLeaguesPage getPlayersStatsByLeagues(String seasonId) {
        Season season = getSeasonOrThrow(seasonsRepository, seasonId);
        List<League> leagues = season.getLeagues();
        List<LeagueStatsPage> playersStatsByLeagues = new ArrayList<>();

        for (League league : leagues) {
            List<PlayerStatsDto> playersStatsByLeague = getPlayersStatsByLeague(league);
            playersStatsByLeagues.add(new LeagueStatsPage(league, playersStatsByLeague));
        }
        return new PlayersStatsByLeaguesPage(playersStatsByLeagues);
    }

    // ------------------------------------------------------------------------------------------------------ //

    //@Override
    public List<PlayerStatsDto> getPlayersStatsByLeague(League league) {
        Map<String, PlayerStatsDto> playersStatsByLeagueMap = new HashMap<>();
        List<Bet> bets = league.getBets();
        for (Bet bet : bets) {
            if (bet.getBetStatus().equals(Bet.BetStatus.DELETED)) {
                continue;
            }
            String username = bet.getUser().getUsername();

            PlayerStatsDto playerStats = playersStatsByLeagueMap.getOrDefault(
                    username, getDefaultPlayerStatsDto(bet.getUser().getAvatar(), username));

            if (bet.getBetStatus().equals(Bet.BetStatus.OPENED)) {
                playerStats.setTotalBets(playerStats.getTotalBets() + 1);
                continue;
            }

            Double balanceChange = 0.0;
            if (bet.getBalanceChange() != null) {
                balanceChange = bet.getBalanceChange();
            }
            Double odds = 0.0;
            if (bet.getBetOdds() != null) {
                odds = bet.getBetOdds();
            }
            playerStats.setActualBalance(playerStats.getActualBalance() + balanceChange);
            playerStats.setTotalBets(playerStats.getTotalBets() + 1);
            playerStats.setBetCount(playerStats.getBetCount() + 1);
            playerStats.setSumOfOdds(playerStats.getSumOfOdds() + odds);

            if (bet.getBetStatus().equals(Bet.BetStatus.WON)) {
                playerStats.setWonBetCount(playerStats.getWonBetCount() + 1);
                playerStats.setSumOfWonOdds(playerStats.getSumOfWonOdds() + odds);
            }
            if (bet.getBetStatus().equals(Bet.BetStatus.RETURNED)) {
                playerStats.setReturnedBetCount(playerStats.getReturnedBetCount() + 1);
            }
            if (bet.getBetStatus().equals(Bet.BetStatus.LOST)) {
                playerStats.setLostBetCount(playerStats.getLostBetCount() + 1);
            }
            if (bet.getBetStatus().equals(Bet.BetStatus.EMPTY)) {
                playerStats.setEmptyBetCount(playerStats.getEmptyBetCount() + 1);
            }

            playersStatsByLeagueMap.put(username, playerStats);
        }

        for (PlayerStatsDto playerStats : playersStatsByLeagueMap.values()) {
            playerStats.setAverageOdds(playerStats.getAverageOdds());
            playerStats.setAverageWonBetOdds(playerStats.getAverageWonBetOdds());
            if (playerStats.getBetCount() - playerStats.getReturnedBetCount() - playerStats.getEmptyBetCount() == 0) {
                playerStats.setWinRate(0.0);
            } else {
                playerStats.setWinRate(playerStats.calculateWinRate());
            }
        }

        return new ArrayList<>(playersStatsByLeagueMap.values());
    }

    // ------------------------------------------------------------------------------------------------------ //

    private PlayerStatsDto getDefaultPlayerStatsDto(String avatar, String username) {
        return PlayerStatsDto.builder()
                .avatar(avatar)
                .username(username)
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
