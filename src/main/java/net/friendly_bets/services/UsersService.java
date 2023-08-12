package net.friendly_bets.services;

import net.friendly_bets.dto.NewPasswordUpdateDto;
import net.friendly_bets.dto.PlayersStatsPage;
import net.friendly_bets.dto.UserDto;

public interface UsersService {

    UserDto getProfile(String currentUserId);

    UserDto editEmail(String currentUserId, String newEmail);

    UserDto editPassword(String currentUserId, NewPasswordUpdateDto newPasswordUpdateDto);

    UserDto editUsername(String currentUserId, String newUsername);

    PlayersStatsPage getPlayersStatsBySeason(String seasonId);

}
