package net.friendly_bets.services.impl;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import net.friendly_bets.dto.UpdatedEmailDto;
import net.friendly_bets.dto.UpdatedPasswordDto;
import net.friendly_bets.dto.UpdatedUsernameDto;
import net.friendly_bets.dto.UserDto;
import net.friendly_bets.exceptions.BadRequestException;
import net.friendly_bets.exceptions.ConflictException;
import net.friendly_bets.models.User;
import net.friendly_bets.repositories.UsersRepository;
import net.friendly_bets.services.UsersService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import static net.friendly_bets.utils.GetEntityOrThrow.getUserOrThrow;

@RequiredArgsConstructor
@Service
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class UsersServiceImpl implements UsersService {

    UsersRepository usersRepository;
    PasswordEncoder passwordEncoder;

    @Override
    public UserDto getProfile(String currentUserId) {
        User user = getUserOrThrow(usersRepository, currentUserId);
        return UserDto.from(user);
    }

    // ------------------------------------------------------------------------------------------------------ //

    @Override
    @Transactional
    public UserDto editEmail(String currentUserId, UpdatedEmailDto updatedEmailDto) {
        User user = getUserOrThrow(usersRepository, currentUserId);
        if (user.getEmail().equals(updatedEmailDto.getNewEmail().toLowerCase())) {
            throw new ConflictException("Новый E-mail совпадает со старым");
        }
        if (usersRepository.existsByEmail(updatedEmailDto.getNewEmail().toLowerCase())) {
            throw new ConflictException("Пользователь с таким E-mail уже существует");
        }

        user.setEmail(updatedEmailDto.getNewEmail().toLowerCase());
        usersRepository.save(user);

        return UserDto.from(user);
    }

    // ------------------------------------------------------------------------------------------------------ //

    @Override
    @Transactional
    public UserDto editPassword(String currentUserId, UpdatedPasswordDto updatedPasswordDto) {
        if (updatedPasswordDto.getNewPassword().equals(updatedPasswordDto.getCurrentPassword())) {
            throw new BadRequestException("Введенные текущий и новый пароль совпадают");
        }

        User user = getUserOrThrow(usersRepository, currentUserId);

        if (!passwordEncoder.matches(updatedPasswordDto.getCurrentPassword(), user.getHashPassword())) {
            throw new BadRequestException("Введенный текущий пароль указан неверно");
        }
        if (passwordEncoder.matches(updatedPasswordDto.getNewPassword(), user.getHashPassword())) {
            throw new ConflictException("Новый пароль совпадает с текущим");
        }

        user.setHashPassword(passwordEncoder.encode(updatedPasswordDto.getNewPassword()));
        usersRepository.save(user);

        return UserDto.from(user);
    }

    // ------------------------------------------------------------------------------------------------------ //

    @Override
    @Transactional
    public UserDto editUsername(String currentUserId, UpdatedUsernameDto updatedUsernameDto) {
        User user = getUserOrThrow(usersRepository, currentUserId);
        if (user.getUsername() != null && user.getUsername().equals(updatedUsernameDto.getNewUsername())) {
            throw new BadRequestException("Новое имя совпадает со старым");
        }
        if (usersRepository.existsByUsername(updatedUsernameDto.getNewUsername())) {
            throw new ConflictException("Пользователь с таким именем уже существует. Выберите себе другое имя");
        }

        user.setUsername(updatedUsernameDto.getNewUsername());
        usersRepository.save(user);

        return UserDto.from(user);
    }

    // ------------------------------------------------------------------------------------------------------ //

}
