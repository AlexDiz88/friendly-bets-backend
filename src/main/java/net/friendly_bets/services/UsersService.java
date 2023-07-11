package net.friendly_bets.services;

import net.friendly_bets.dto.UserDto;

public interface UsersService {

    UserDto getProfile(String currentUserId);

}
