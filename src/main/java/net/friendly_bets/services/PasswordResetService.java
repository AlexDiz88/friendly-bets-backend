package net.friendly_bets.services;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import net.friendly_bets.dto.ResetPasswordDto;
import net.friendly_bets.exceptions.BadRequestException;
import net.friendly_bets.models.AccountToken;
import net.friendly_bets.models.AccountTokenType;
import net.friendly_bets.models.User;
import net.friendly_bets.repositories.UsersRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class PasswordResetService {

    UsersRepository usersRepository;
    AccountTokenService accountTokenService;
    AuthEmailService authEmailService;
    AuthRateLimitService authRateLimitService;
    PasswordEncoder passwordEncoder;

    @Transactional
    public void requestPasswordReset(String email) {
        String normalizedEmail = email.trim().toLowerCase();
        authRateLimitService.checkAndIncrement("forgot-password:" + normalizedEmail);

        usersRepository.findByEmail(normalizedEmail).ifPresent(user -> {
            if (Boolean.TRUE.equals(user.getEmailIsConfirmed())) {
                String rawToken = accountTokenService.createToken(user.getId(), AccountTokenType.PASSWORD_RESET);
                authEmailService.sendPasswordReset(user.getEmail(), rawToken);
            }
        });
    }

    @Transactional
    public void resetPassword(ResetPasswordDto dto) {
        if (!dto.getPassword().equals(dto.getPasswordRepeat())) {
            throw new BadRequestException("newAndRepeatPasswordsNotEqual");
        }

        AccountToken token = accountTokenService.consumeToken(dto.getToken(), AccountTokenType.PASSWORD_RESET);
        User user = usersRepository.findById(token.getUserId())
                .orElseThrow(() -> new BadRequestException("invalidOrExpiredToken"));

        if (!Boolean.TRUE.equals(user.getEmailIsConfirmed())) {
            throw new BadRequestException("emailNotConfirmed");
        }

        user.setHashPassword(passwordEncoder.encode(dto.getPassword()));
        usersRepository.save(user);
    }
}
