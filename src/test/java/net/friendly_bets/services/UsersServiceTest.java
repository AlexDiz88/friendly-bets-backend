package net.friendly_bets.services;

import net.friendly_bets.dto.UpdatedEmailDto;
import net.friendly_bets.dto.UpdatedPasswordDto;
import net.friendly_bets.dto.UpdatedUsernameDto;
import net.friendly_bets.dto.UserDto;
import net.friendly_bets.exceptions.BadRequestException;
import net.friendly_bets.exceptions.ConflictException;
import net.friendly_bets.models.User;
import net.friendly_bets.repositories.UsersRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UsersServiceTest {

    @Mock
    private UsersRepository usersRepository;
    @Mock
    private GetEntityService getEntityService;
    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private UsersService usersService;

    private final String userId = "userId";
    private final String email = "test@example.com";
    private final String currentPassword = "currentPassword";
    private final String newPassword = "newPassword";
    private final String currentUsername = "testUser";
    private User user;
    private UserDto userDto;

    @BeforeEach
    void setUp() {
        user = User.builder()
                .id(userId)
                .email(email)
                .hashPassword("hashedPassword")
                .role(User.Role.USER)
                .username(currentUsername)
                .language("en")
                .build();

        userDto = UserDto.from(user);
    }

    @Test
    @DisplayName("Should return UserDto when user exists")
    void getProfile_ReturnsUserDto_WhenUserExists() {
        // given
        when(getEntityService.getUserOrThrow(userId)).thenReturn(user);

        // when
        UserDto result = usersService.getProfile(userId);

        // then
        assertNotNull(result);
        assertEquals(userDto, result);
        verify(getEntityService, times(1)).getUserOrThrow(userId);
    }

    // ------------------------------------------------------------------------------------------------------ //

    @Test
    @DisplayName("Should return updated UserDto with new email and not modified other fields when email is changed")
    void editEmail_ReturnsUpdatedUserDto_WhenEmailIsChanged() {
        // given
        String expectedNewEmail = "new@example.com";
        UpdatedEmailDto updatedEmailDto = new UpdatedEmailDto(expectedNewEmail);
        when(getEntityService.getUserOrThrow(userId)).thenReturn(user);
        when(usersRepository.existsByEmail(expectedNewEmail)).thenReturn(false);
        when(usersRepository.save(any(User.class))).thenReturn(user);

        // when
        UserDto actualResult = usersService.editEmail(userId, updatedEmailDto);

        // then
        assertNotNull(actualResult);
        assertEquals(expectedNewEmail, actualResult.getEmail());
        assertEquals(user.getId(), actualResult.getId());
        assertEquals(user.getUsername(), actualResult.getUsername());
        assertEquals(user.getRole().toString(), actualResult.getRole());

        verify(usersRepository, times(1)).existsByEmail(expectedNewEmail);
        verify(usersRepository, times(1)).save(user);
    }

    @Test
    @DisplayName("Should throw ConflictException when new email matches current email")
    void editEmail_ThrowsConflictException_WhenEmailMatchesCurrent() {
        // given
        UpdatedEmailDto updatedEmailDto = new UpdatedEmailDto(email);
        when(getEntityService.getUserOrThrow(userId)).thenReturn(user);

        // when + then
        ConflictException exception = assertThrows(ConflictException.class, () -> usersService.editEmail(userId, updatedEmailDto));

        assertEquals("newAndOldEmailsAreSame", exception.getMessage());
        verify(usersRepository, never()).save(any(User.class));
    }

    @Test
    @DisplayName("Should throw ConflictException when new email is already in use by another user")
    void editEmail_ThrowsConflictException_WhenNewEmailIsAlreadyInUse() {
        // given
        String newEmail = "new@example.com";
        UpdatedEmailDto updatedEmailDto = new UpdatedEmailDto(newEmail);
        when(getEntityService.getUserOrThrow(userId)).thenReturn(user);
        when(usersRepository.existsByEmail(newEmail)).thenReturn(true);

        // when + then
        ConflictException exception = assertThrows(ConflictException.class, () -> usersService.editEmail(userId, updatedEmailDto));

        assertEquals("userWithThisEmailAlreadyExist", exception.getMessage());
        verify(usersRepository, never()).save(any(User.class));
    }

    // ------------------------------------------------------------------------------------------------------ //

    @Test
    @DisplayName("Should return updated UserDto with new password and not modified other fields when password is changed")
    void editPassword_ReturnsUpdatedUserDto_WhenPasswordIsChanged() {
        // given
        String encodedNewPassword = "encodedNewPassword";
        UpdatedPasswordDto updatedPasswordDto = new UpdatedPasswordDto(currentPassword, newPassword);

        when(getEntityService.getUserOrThrow(userId)).thenReturn(user);
        when(passwordEncoder.matches(currentPassword, user.getHashPassword())).thenReturn(true);
        when(passwordEncoder.matches(newPassword, user.getHashPassword())).thenReturn(false);
        when(passwordEncoder.encode(newPassword)).thenReturn(encodedNewPassword);

        // when
        UserDto actualResult = usersService.editPassword(userId, updatedPasswordDto);

        // then
        assertNotNull(actualResult);
        assertEquals(user.getHashPassword(), encodedNewPassword);
        assertEquals(user.getId(), actualResult.getId());
        assertEquals(user.getUsername(), actualResult.getUsername());
        assertEquals(user.getEmail(), actualResult.getEmail());
        assertEquals(user.getRole().toString(), actualResult.getRole());

        verify(passwordEncoder, times(1)).encode(updatedPasswordDto.getNewPassword());
        verify(usersRepository, times(1)).save(user);
    }

    @Test
    @DisplayName("Should throw BadRequestException when new password matches current password")
    void editPassword_ThrowsBadRequestException_WhenPasswordsAreSame() {
        // given
        UpdatedPasswordDto updatedPasswordDto = new UpdatedPasswordDto(currentPassword, currentPassword);

        // when + then
        BadRequestException exception = assertThrows(BadRequestException.class, () -> usersService.editPassword(userId, updatedPasswordDto));

        assertEquals("enteredPasswordsAreSame", exception.getMessage());
        verify(usersRepository, never()).save(any(User.class));
    }

    @Test
    @DisplayName("Should throw BadRequestException when current password is incorrect")
    void editPassword_ThrowsBadRequestException_WhenCurrentPasswordIsIncorrect() {
        // given
        UpdatedPasswordDto updatedPasswordDto = new UpdatedPasswordDto(currentPassword, newPassword);

        when(getEntityService.getUserOrThrow(userId)).thenReturn(user);
        when(passwordEncoder.matches(currentPassword, user.getHashPassword())).thenReturn(false);

        // when + then
        BadRequestException exception = assertThrows(BadRequestException.class, () -> usersService.editPassword(userId, updatedPasswordDto));

        assertEquals("actualPasswordNotCorrect", exception.getMessage());
        verify(usersRepository, never()).save(any(User.class));
    }

    @Test
    @DisplayName("Should throw ConflictException when new password matches hashed old password")
    void editPassword_ThrowsConflictException_WhenNewPasswordMatchesHashedOldPassword() {
        // given
        UpdatedPasswordDto updatedPasswordDto = new UpdatedPasswordDto(currentPassword, newPassword);

        when(getEntityService.getUserOrThrow(userId)).thenReturn(user);
        when(passwordEncoder.matches(currentPassword, user.getHashPassword())).thenReturn(true);
        when(passwordEncoder.matches(newPassword, user.getHashPassword())).thenReturn(true);

        // when + then
        ConflictException exception = assertThrows(ConflictException.class, () -> usersService.editPassword(userId, updatedPasswordDto));

        assertEquals("newAndOldPasswordsAreSame", exception.getMessage());
        verify(usersRepository, never()).save(any(User.class));
    }

    // ------------------------------------------------------------------------------------------------------ //

    @Test
    @DisplayName("Should return updated UserDto with new username and not modified other fields when username is changed")
    void editUsername_ReturnsUpdatedUserDto_WhenUsernameIsChanged() {
        // given
        String expectedNewUsername = "newUsername";
        UpdatedUsernameDto updatedUsernameDto = new UpdatedUsernameDto(expectedNewUsername);
        when(getEntityService.getUserOrThrow(userId)).thenReturn(user);
        when(usersRepository.existsByUsername(expectedNewUsername)).thenReturn(false);
        when(usersRepository.save(any(User.class))).thenReturn(user);

        // when
        UserDto actualResult = usersService.editUsername(userId, updatedUsernameDto);

        // then
        assertNotNull(actualResult);
        assertEquals(expectedNewUsername, actualResult.getUsername());
        assertEquals(user.getId(), actualResult.getId());
        assertEquals(user.getEmail(), actualResult.getEmail());
        assertEquals(user.getRole().toString(), actualResult.getRole());

        verify(usersRepository, times(1)).existsByUsername(expectedNewUsername);
        verify(usersRepository, times(1)).save(user);
    }

    @Test
    @DisplayName("Should throw BadRequestException when new username matches current username")
    void editUsername_ThrowsBadRequestException_WhenUsernameMatchesCurrent() {
        // given
        UpdatedUsernameDto updatedUsernameDto = new UpdatedUsernameDto(currentUsername);
        when(getEntityService.getUserOrThrow(userId)).thenReturn(user);

        // when + then
        BadRequestException exception = assertThrows(BadRequestException.class,
                () -> usersService.editUsername(userId, updatedUsernameDto));

        assertEquals("newAndOldUsernamesAreSame", exception.getMessage());
        verify(usersRepository, never()).save(any(User.class));
    }

    @Test
    @DisplayName("Should throw ConflictException when new username is already in use by another user")
    void editUsername_ThrowsConflictException_WhenNewUsernameIsAlreadyInUse() {
        // given
        String newUsername = "newUsername";
        UpdatedUsernameDto updatedUsernameDto = new UpdatedUsernameDto(newUsername);

        when(getEntityService.getUserOrThrow(userId)).thenReturn(user);
        when(usersRepository.existsByUsername(newUsername)).thenReturn(true);

        // when + then
        ConflictException exception = assertThrows(ConflictException.class,
                () -> usersService.editUsername(userId, updatedUsernameDto));

        assertEquals("usernameAlreadyExist", exception.getMessage());
        verify(usersRepository, never()).save(any(User.class));
    }

    // ------------------------------------------------------------------------------------------------------ //

    @ParameterizedTest
    @DisplayName("Should return updated UserDto with new language and not modified other fields when a supported language is changed")
    @MethodSource("supportedLanguagesProvider")
    void changeLanguage_ReturnsUpdatedUserDto_WhenLanguageIsChanged(String newLanguage) {
        // given
        when(getEntityService.getUserOrThrow(userId)).thenReturn(user);
        when(usersRepository.save(any(User.class))).thenReturn(user);

        // when
        UserDto actualResult = usersService.changeLanguage(userId, newLanguage);

        // then
        assertNotNull(actualResult);
        assertEquals(newLanguage, actualResult.getLanguage());
        assertEquals(user.getId(), actualResult.getId());
        assertEquals(user.getUsername(), actualResult.getUsername());
        assertEquals(user.getRole().toString(), actualResult.getRole());

        verify(usersRepository, times(1)).save(user);
    }

    private static Stream<String> supportedLanguagesProvider() {
        return Stream.of("en", "de", "ru", "EN", "DE", "RU", " en ", " de", "ru   ");
    }

    @ParameterizedTest
    @DisplayName("Should throw BadRequestException when language is not supported")
    @MethodSource("unsupportedLanguagesProvider")
    void changeLanguage_ThrowsBadRequestException_WhenLanguageIsNotSupported(String unsupportedLanguage) {
        // given
        when(getEntityService.getUserOrThrow(userId)).thenReturn(user);

        // when + then
        BadRequestException exception = assertThrows(BadRequestException.class,
                () -> usersService.changeLanguage(userId, unsupportedLanguage));

        assertEquals("languageNotSupported", exception.getMessage());
        verify(usersRepository, never()).save(any(User.class));
    }

    private static Stream<String> unsupportedLanguagesProvider() {
        return Stream.of("-en", "_de_", "'ru'", "eng", "Deutsch", "ру");
    }

}
