package net.friendly_bets.services;

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
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import static net.friendly_bets.utils.Constants.SUPPORTED_LANGUAGES;

@RequiredArgsConstructor
@Service
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class UsersService {

    UsersRepository usersRepository;
    PasswordEncoder passwordEncoder;
    GetEntityService getEntityService;

    public UserDto getProfile(String currentUserId) {
        User user = getEntityService.getUserOrThrow(currentUserId);
        return UserDto.from(user);
    }

    // ------------------------------------------------------------------------------------------------------ //

    @Transactional
    public UserDto editEmail(String currentUserId, UpdatedEmailDto updatedEmailDto) {
        User user = getEntityService.getUserOrThrow(currentUserId);
        if (user.getEmail().equals(updatedEmailDto.getNewEmail().toLowerCase())) {
            throw new ConflictException("newAndOldEmailsAreSame");
        }
        if (usersRepository.existsByEmail(updatedEmailDto.getNewEmail().toLowerCase())) {
            throw new ConflictException("userWithThisEmailAlreadyExist");
        }

        user.setEmail(updatedEmailDto.getNewEmail().toLowerCase());
        usersRepository.save(user);

        return UserDto.from(user);
    }

    // ------------------------------------------------------------------------------------------------------ //

    @Transactional
    public UserDto editPassword(String currentUserId, UpdatedPasswordDto updatedPasswordDto) {
        if (updatedPasswordDto.getNewPassword().equals(updatedPasswordDto.getCurrentPassword())) {
            throw new BadRequestException("enteredPasswordsAreSame");
        }

        User user = getEntityService.getUserOrThrow(currentUserId);

        if (!passwordEncoder.matches(updatedPasswordDto.getCurrentPassword(), user.getHashPassword())) {
            throw new BadRequestException("actualPasswordNotCorrect");
        }
        if (passwordEncoder.matches(updatedPasswordDto.getNewPassword(), user.getHashPassword())) {
            throw new ConflictException("newAndOldPasswordsAreSame");
        }

        user.setHashPassword(passwordEncoder.encode(updatedPasswordDto.getNewPassword()));
        usersRepository.save(user);

        return UserDto.from(user);
    }

    // ------------------------------------------------------------------------------------------------------ //

    @Transactional
    public UserDto editUsername(String currentUserId, UpdatedUsernameDto updatedUsernameDto) {
        User user = getEntityService.getUserOrThrow(currentUserId);
        if (user.getUsername() != null && user.getUsername().equals(updatedUsernameDto.getNewUsername())) {
            throw new BadRequestException("newAndOldUsernamesAreSame");
        }
        if (usersRepository.existsByUsername(updatedUsernameDto.getNewUsername())) {
            throw new ConflictException("usernameAlreadyExist");
        }

        user.setUsername(updatedUsernameDto.getNewUsername());
        usersRepository.save(user);

        return UserDto.from(user);
    }

    // ------------------------------------------------------------------------------------------------------ //

    @Transactional
    public UserDto changeLanguage(String currentUserId, String language) {
        User user = getEntityService.getUserOrThrow(currentUserId);

        if (!SUPPORTED_LANGUAGES.contains(language.trim().toLowerCase())) {
            throw new BadRequestException("languageNotSupported");
        }

        user.setLanguage(language);
        usersRepository.save(user);

        return UserDto.from(user);
    }

    // ------------------------------------------------------------------------------------------------------ //

}
