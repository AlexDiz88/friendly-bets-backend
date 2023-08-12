package net.friendly_bets.services.impl;

import lombok.RequiredArgsConstructor;
import net.friendly_bets.dto.NewPasswordUpdateDto;
import net.friendly_bets.dto.PlayerStatsDto;
import net.friendly_bets.dto.PlayersStatsPage;
import net.friendly_bets.dto.UserDto;
import net.friendly_bets.exceptions.BadDataException;
import net.friendly_bets.exceptions.ConflictException;
import net.friendly_bets.exceptions.NotFoundException;
import net.friendly_bets.models.Bet;
import net.friendly_bets.models.League;
import net.friendly_bets.models.Season;
import net.friendly_bets.models.User;
import net.friendly_bets.repositories.SeasonsRepository;
import net.friendly_bets.repositories.UsersRepository;
import net.friendly_bets.services.UsersService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static net.friendly_bets.utils.FieldsValidator.isValidEmail;
import static net.friendly_bets.utils.FieldsValidator.isValidUsername;

@RequiredArgsConstructor
@Service
public class UsersServiceImpl implements UsersService {

    private final UsersRepository usersRepository;
    private final SeasonsRepository seasonsRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public UserDto getProfile(String currentUserId) {
        User user = usersRepository.findById(currentUserId)
                .orElseThrow(IllegalArgumentException::new);

        return UserDto.from(user);
    }

    // ------------------------------------------------------------------------------------------------------ //

    @Override
    public UserDto editEmail(String currentUserId, String newEmail) {
        if (newEmail == null || newEmail.trim().length() < 1) {
            throw new BadDataException("Email не должен быть пустым");
        }
        if (newEmail.length() < 6 || !isValidEmail(newEmail)) {
            throw new BadDataException("Введенный e-mail некорректен");
        }
        User user = usersRepository.findById(currentUserId)
                .orElseThrow(IllegalArgumentException::new);
        if (user.getEmail().equals(newEmail.toLowerCase())) {
            throw new BadDataException("Новый E-mail совпадает со старым");
        }
        if (usersRepository.existsByEmail(newEmail.toLowerCase())) {
            throw new ConflictException("Пользователь с таким E-mail уже существует");
        }

        user.setEmail(newEmail.toLowerCase());
        usersRepository.save(user);

        return UserDto.from(user);
    }

    // ------------------------------------------------------------------------------------------------------ //

    @Override
    public UserDto editPassword(String currentUserId, NewPasswordUpdateDto newPasswordUpdateDto) {

        if (newPasswordUpdateDto.getCurrentPassword() == null || newPasswordUpdateDto.getCurrentPassword().length() < 1) {
            throw new BadDataException("Введенный текущий пароль не должен быть пустым");
        }
        if (newPasswordUpdateDto.getNewPassword() == null || newPasswordUpdateDto.getNewPassword().length() < 1) {
            throw new BadDataException("Введенный новый пароль не должен быть пустым");
        }
        if (newPasswordUpdateDto.getNewPassword().equals(newPasswordUpdateDto.getCurrentPassword())) {
            throw new BadDataException("Введенные текущий и новый пароль совпадают");
        }
        if (newPasswordUpdateDto.getNewPassword().length() < 6) {
            throw new BadDataException("Новый пароль должен быть длиной не менее 6 символов");
        }

        User user = usersRepository.findById(currentUserId).orElseThrow(IllegalArgumentException::new);

        if (!passwordEncoder.matches(newPasswordUpdateDto.getCurrentPassword(), user.getHashPassword())) {
            throw new BadDataException("Введенный текущий пароль указан неверно");
        }
        if (passwordEncoder.matches(newPasswordUpdateDto.getNewPassword(), user.getHashPassword())) {
            throw new BadDataException("Новый пароль совпадает с текущим");
        }

        user.setHashPassword(passwordEncoder.encode(newPasswordUpdateDto.getNewPassword()));
        usersRepository.save(user);

        return UserDto.from(user);
    }

    // ------------------------------------------------------------------------------------------------------ //

    @Override
    public UserDto editUsername(String currentUserId, String newUsername) {
        if (newUsername == null || newUsername.trim().length() < 1) {
            throw new BadDataException("Имя не должно быть пустым");
        }
        if (newUsername.trim().length() < 2) {
            throw new BadDataException("Длина имени должна быть не менее 2 символов");
        }
        if (!isValidUsername(newUsername)) {
            throw new BadDataException("Имя является некорректным. Имя должно начинаться с буквы. В имени можно использовать только буквы, цифры и пробел, а также символы - и _");
        }
        User user = usersRepository.findById(currentUserId)
                .orElseThrow(IllegalArgumentException::new);
        if (user.getUsername() != null && user.getUsername().equals(newUsername)) {
            throw new BadDataException("Новое имя совпадает со старым");
        }
        if (usersRepository.existsByUsername(newUsername)) {
            throw new ConflictException("Пользователь с таким именем уже существует. Выберите себе другое имя");
        }

        user.setUsername(newUsername);
        usersRepository.save(user);

        return UserDto.from(user);
    }

    // ------------------------------------------------------------------------------------------------------ //

    @Override
    public PlayersStatsPage getPlayersStatsBySeason(String seasonId) {
        Season season = seasonsRepository.findById(seasonId).orElseThrow(
                () -> new NotFoundException("Сезон с ID <" + seasonId + "> не найден")
        );
        List<League> leagues = season.getLeagues();
        Map<String, PlayerStatsDto> playersStatsMap = new HashMap<>();

        for (League league : leagues) {
            List<Bet> bets = league.getBets();
            for (Bet bet : bets) {
                if (bet.getBetStatus().equals(Bet.BetStatus.OPENED)) {
                    continue;
                }

                String username = bet.getUser().getUsername();

                PlayerStatsDto playerStats = playersStatsMap.getOrDefault(
                        username,
                        PlayerStatsDto.builder()
                                .username(username)
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
                                .build());

                Double balanceChange = 0.0;
                if (bet.getBalanceChange() != null) {
                    balanceChange = bet.getBalanceChange();
                }
                Double odds = 0.0;
                if (bet.getBetOdds() != null) {
                    odds = bet.getBetOdds();
                }
                playerStats.setActualBalance(playerStats.getActualBalance() + balanceChange);
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

                playersStatsMap.put(username, playerStats);
            }
        }

        for (PlayerStatsDto playerStats : playersStatsMap.values()) {
            double averageOdds = playerStats.getAverageOdds();
            playerStats.setAverageOdds(averageOdds);
            double averageWonOdds = playerStats.getAverageWonOdds();
            playerStats.setAverageWonBetOdds(averageWonOdds);
            if (playerStats.getBetCount() - playerStats.getReturnedBetCount() - playerStats.getEmptyBetCount() == 0) {
                playerStats.setWinRate(0.0);
            } else {
                double winRate = 100.0 * playerStats.getWonBetCount() / (playerStats.getBetCount() - playerStats.getReturnedBetCount() - playerStats.getEmptyBetCount());
                playerStats.setWinRate(winRate);
            }
        }

        List<PlayerStatsDto> playerStatsList = new ArrayList<>(playersStatsMap.values());
        return new PlayersStatsPage(playerStatsList);
    }

    // ------------------------------------------------------------------------------------------------------ //


}
