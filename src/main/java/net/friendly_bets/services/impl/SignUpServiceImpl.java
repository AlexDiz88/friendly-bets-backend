package net.friendly_bets.services.impl;

import lombok.RequiredArgsConstructor;
import net.friendly_bets.dto.NewUserDto;
import net.friendly_bets.dto.UserDto;
import net.friendly_bets.exceptions.ConflictException;
import net.friendly_bets.models.User;
import net.friendly_bets.repositories.UsersRepository;
import net.friendly_bets.services.SignUpService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@RequiredArgsConstructor
@Service
public class SignUpServiceImpl implements SignUpService {

    private final UsersRepository usersRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    @Transactional
    public UserDto signUp(NewUserDto newUser) {
        if (usersRepository.existsByEmail(newUser.getEmail().toLowerCase())) {
            throw new ConflictException("Пользователь с таким e-mail уже зарегистрирован");
        }
        User user = User.builder()
                .createdAt(LocalDateTime.now())
                .email(newUser.getEmail().toLowerCase())
                .emailIsConfirmed(false)
                .hashPassword(passwordEncoder.encode(newUser.getPassword()))
                .role(User.Role.USER)
                .build();

        usersRepository.save(user);

        return UserDto.from(user);
    }
}
