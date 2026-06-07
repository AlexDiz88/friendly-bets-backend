package net.friendly_bets.services;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import net.friendly_bets.config.AppAuthProperties;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class AuthEmailService {

    MailDeliveryService mailDeliveryService;
    AppAuthProperties appAuthProperties;

    public void sendEmailVerification(String email, String rawToken) {
        String link = buildFrontendLink("/auth/verify-email", rawToken);
        mailDeliveryService.sendPlainText(
                email,
                "FriendlyBets — подтверждение email",
                """
                        Здравствуйте!

                        Для подтверждения адреса email перейдите по ссылке (действует ограниченное время):
                        %s

                        Если вы не регистрировались на FriendlyBets, проигнорируйте это письмо.
                        """.formatted(link)
        );
    }

    public void sendPasswordReset(String email, String rawToken) {
        String link = buildFrontendLink("/auth/reset-password", rawToken);
        mailDeliveryService.sendPlainText(
                email,
                "FriendlyBets — сброс пароля",
                """
                        Здравствуйте!

                        Для сброса пароля перейдите по ссылке (действует ограниченное время):
                        %s

                        Если вы не запрашивали сброс пароля, проигнорируйте это письмо.
                        """.formatted(link)
        );
    }

    private String buildFrontendLink(String path, String rawToken) {
        String base = appAuthProperties.getFrontendBaseUrl().replaceAll("/$", "");
        return base + "/#" + path + "?token=" + rawToken;
    }
}
