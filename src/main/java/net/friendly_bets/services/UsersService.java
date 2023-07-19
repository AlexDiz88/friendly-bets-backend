package net.friendly_bets.services;

import net.friendly_bets.dto.UserDto;

public interface UsersService {

    UserDto getProfile(String currentUserId);

    UserDto editEmail(String currentUserId, String newEmail);

    UserDto editUsername(String currentUserId, String newUsername);

}
