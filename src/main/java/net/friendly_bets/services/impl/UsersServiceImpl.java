package net.friendly_bets.services.impl;

import lombok.RequiredArgsConstructor;
import net.friendly_bets.dto.UserDto;
import net.friendly_bets.exceptions.BadDataException;
import net.friendly_bets.exceptions.ConflictException;
import net.friendly_bets.models.Season;
import net.friendly_bets.models.User;
import net.friendly_bets.repositories.SeasonsRepository;
import net.friendly_bets.repositories.UsersRepository;
import net.friendly_bets.services.UsersService;
import org.springframework.stereotype.Service;

import java.util.List;

@RequiredArgsConstructor
@Service
public class UsersServiceImpl implements UsersService {

    private final UsersRepository usersRepository;

    @Override
    public UserDto getProfile(String currentUserId) {
        User user = usersRepository.findById(currentUserId)
                .orElseThrow(IllegalArgumentException::new);

        return UserDto.from(user);
    }

    // ------------------------------------------------------------------------------------------------------ //

    @Override
    public UserDto editEmail(String currentUserId, String newEmail) {
        if (newEmail == null || newEmail.trim().length() < 1) {
            throw new BadDataException("Email не должен быть пустым");
        }
        User user = usersRepository.findById(currentUserId)
                .orElseThrow(IllegalArgumentException::new);
        if (user.getEmail() != null && user.getEmail().equals(newEmail)) {
            throw new BadDataException("Новый E-mail совпадает со старым");
        }
        if (usersRepository.existsByEmailEquals(newEmail)) {
            throw new ConflictException("Пользователь с таким E-mail уже существует");
        }

        user.setEmail(newEmail);
        usersRepository.save(user);

        return UserDto.from(user);
    }

    // ------------------------------------------------------------------------------------------------------ //

    @Override
    public UserDto editUsername(String currentUserId, String newUsername) {
        if (newUsername == null || newUsername.trim().length() < 1) {
            throw new BadDataException("Имя не должно быть пустым");
        }
        User user = usersRepository.findById(currentUserId)
                .orElseThrow(IllegalArgumentException::new);
        if (user.getUsername() != null && user.getUsername().equals(newUsername)) {
            throw new BadDataException("Новое имя совпадает со старым");
        }
        if (usersRepository.existsByUsernameEquals(newUsername)) {
            throw new ConflictException("Пользователь с таким именем уже существует");
        }

        user.setUsername(newUsername);
        usersRepository.save(user);

        return UserDto.from(user);
    }

    // ------------------------------------------------------------------------------------------------------ //


}
