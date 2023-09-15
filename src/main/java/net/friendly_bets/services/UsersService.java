package net.friendly_bets.services;

import net.friendly_bets.dto.UpdatedEmailDto;
import net.friendly_bets.dto.UpdatedPasswordDto;
import net.friendly_bets.dto.UpdatedUsernameDto;
import net.friendly_bets.dto.UserDto;

public interface UsersService {

    UserDto getProfile(String currentUserId);

    UserDto editEmail(String currentUserId, UpdatedEmailDto updatedEmailDto);

    UserDto editPassword(String currentUserId, UpdatedPasswordDto updatedPasswordDto);

    UserDto editUsername(String currentUserId, UpdatedUsernameDto updatedUsernameDto);
}
