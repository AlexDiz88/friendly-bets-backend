package net.friendly_bets.services;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import net.friendly_bets.config.AppMailProperties;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Slf4j
@RequiredArgsConstructor
@Service
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class MailDeliveryService {

    ObjectProvider<JavaMailSender> javaMailSenderProvider;
    AppMailProperties appMailProperties;

    public void sendPlainText(String to, String subject, String body) {
        if (!appMailProperties.isEnabled()) {
            log.info("Mail disabled. To={} subject={} body={}", to, subject, body);
            return;
        }
        JavaMailSender sender = javaMailSenderProvider.getIfAvailable();
        if (sender == null) {
            log.warn("Mail enabled but JavaMailSender missing. To={} subject={}", to, subject);
            return;
        }
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(appMailProperties.getFrom());
        message.setTo(to);
        message.setSubject(subject);
        message.setText(body);
        sender.send(message);
    }
}
