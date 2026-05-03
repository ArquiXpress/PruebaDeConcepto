package com.arquixpress.marketplace.identity;

import java.time.Instant;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
public class PasswordResetEmailService {
    private static final Logger log = LoggerFactory.getLogger(PasswordResetEmailService.class);

    private final ObjectProvider<JavaMailSender> mailSender;
    private final boolean enabled;
    private final String from;
    private final String resetBaseUrl;

    public PasswordResetEmailService(
            ObjectProvider<JavaMailSender> mailSender,
            @Value("${app.mail.enabled:false}") boolean enabled,
            @Value("${app.mail.from:no-reply@arquixpress.com}") String from,
            @Value("${app.mail.reset-base-url:http://localhost:4200/recuperar-clave}") String resetBaseUrl) {
        this.mailSender = mailSender;
        this.enabled = enabled;
        this.from = from;
        this.resetBaseUrl = resetBaseUrl;
    }

    public void sendPasswordReset(String to, String displayName, String token, Instant expiresAt) {
        String resetUrl = resetBaseUrl + "?token=" + token;
        if (!enabled) {
            log.info("Password reset mail disabled. Token for {} is {}. Link: {}", to, token, resetUrl);
            return;
        }

        JavaMailSender sender = mailSender.getIfAvailable();
        if (sender == null) {
            throw new IllegalStateException("No hay JavaMailSender configurado para enviar correos");
        }

        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(from);
        message.setTo(to);
        message.setSubject("Recuperacion de clave ArquiXpress");
        message.setText("""
                Hola %s,

                Recibimos una solicitud para restablecer tu clave de ArquiXpress.

                Token: %s
                Enlace: %s

                Este token expira en: %s

                Si no solicitaste este cambio, ignora este correo.
                """.formatted(displayName, token, resetUrl, expiresAt));
        sender.send(message);
    }
}
