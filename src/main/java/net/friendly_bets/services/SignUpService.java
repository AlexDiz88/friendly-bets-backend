package net.friendly_bets.services;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import net.friendly_bets.dto.NewUserDto;
import net.friendly_bets.dto.UserDto;
import net.friendly_bets.exceptions.ConflictException;
import net.friendly_bets.models.User;
import net.friendly_bets.repositories.UsersRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@RequiredArgsConstructor
@Service
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class SignUpService {

    UsersRepository usersRepository;
    PasswordEncoder passwordEncoder;

    @Transactional
    public UserDto signUp(NewUserDto newUser) {
        if (usersRepository.existsByEmail(newUser.getEmail().toLowerCase())) {
            throw new ConflictException("userWithThisEmailAlreadyRegistered");
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
