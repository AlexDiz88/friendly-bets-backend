package net.friendly_bets.services;

import net.friendly_bets.dto.PlayersStatsPage;
import net.friendly_bets.dto.UserDto;

public interface UsersService {

    UserDto getProfile(Long currentUserId);

    UserDto editEmail(Long currentUserId, String newEmail);

    UserDto editUsername(Long currentUserId, String newUsername);

    PlayersStatsPage getPlayersStatsBySeason(Long seasonId);

}
