package net.friendly_bets.services;

import net.friendly_bets.dto.*;

public interface UsersService {

    UserDto getProfile(String currentUserId);

    UserDto editEmail(String currentUserId, UpdatedEmailDto updatedEmailDto);

    UserDto editPassword(String currentUserId, UpdatedPasswordDto updatedPasswordDto);

    UserDto editUsername(String currentUserId, UpdatedUsernameDto updatedUsernameDto);

    PlayersStatsPage getPlayersStatsBySeason(String seasonId);

    PlayersStatsByLeaguesPage getPlayersStatsByLeagues(String seasonId);

}
