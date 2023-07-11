package net.friendly_bets.services;


import net.friendly_bets.dto.NewUserDto;
import net.friendly_bets.dto.UserDto;

public interface SignUpService {
    UserDto signUp(NewUserDto newUser);
}
