package net.friendly_bets.services;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import net.friendly_bets.exceptions.BadRequestException;
import net.friendly_bets.models.AccountToken;
import net.friendly_bets.models.AccountTokenType;
import net.friendly_bets.models.User;
import net.friendly_bets.repositories.UsersRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class EmailVerificationService {

    UsersRepository usersRepository;
    AccountTokenService accountTokenService;
    AuthEmailService authEmailService;
    AuthRateLimitService authRateLimitService;

    @Transactional
    public void sendVerificationEmail(User user) {
        if (Boolean.TRUE.equals(user.getEmailIsConfirmed())) {
            return;
        }
        String rawToken = accountTokenService.createToken(user.getId(), AccountTokenType.EMAIL_VERIFICATION);
        authEmailService.sendEmailVerification(user.getEmail(), rawToken);
    }

    @Transactional
    public void confirmEmail(String rawToken) {
        AccountToken token = accountTokenService.consumeToken(rawToken, AccountTokenType.EMAIL_VERIFICATION);
        User user = usersRepository.findById(token.getUserId())
                .orElseThrow(() -> new BadRequestException("invalidOrExpiredToken"));

        user.setEmailIsConfirmed(true);
        usersRepository.save(user);
    }

    @Transactional
    public void resendVerification(String email) {
        String normalizedEmail = email.trim().toLowerCase();
        authRateLimitService.checkAndIncrement("resend-verification:" + normalizedEmail);

        usersRepository.findByEmail(normalizedEmail).ifPresent(user -> {
            if (!Boolean.TRUE.equals(user.getEmailIsConfirmed())) {
                sendVerificationEmail(user);
            }
        });
    }
}
