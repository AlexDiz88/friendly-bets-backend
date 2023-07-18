package net.friendly_bets.services.impl;

import lombok.RequiredArgsConstructor;
import net.friendly_bets.dto.NewUserDto;
import net.friendly_bets.dto.UserDto;
import net.friendly_bets.exceptions.BadDataException;
import net.friendly_bets.exceptions.ConflictException;
import net.friendly_bets.models.User;
import net.friendly_bets.repositories.UsersRepository;
import net.friendly_bets.services.SignUpService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

import static net.friendly_bets.dto.UserDto.from;

@RequiredArgsConstructor
@Service
public class SignUpServiceImpl implements SignUpService {

    private final UsersRepository usersRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public UserDto signUp(NewUserDto newUser) {
        if (newUser.getEmail().length() < 3) {
            throw new BadDataException("E-mail is too short. Must be at least 3 characters long");
        }
        if (usersRepository.existsByEmailEquals(newUser.getEmail())) {
            throw new ConflictException("User with this e-mail already exist");
        }
        if (newUser.getPassword().length() < 3) {
            throw new BadDataException("Password must be at least 3 characters long");
        }
        // TODO change min password length and add pass difficulty check

        User user = User.builder()
                .createdAt(LocalDateTime.now())
                .email(newUser.getEmail())
                .hashPassword(passwordEncoder.encode(newUser.getPassword()))
                .role(User.Role.USER)
                .build();

        usersRepository.save(user);

        return from(user);
    }
}
