package com.dowglasmaia.financecontroller.identity.infrastructure.mail;

import com.dowglasmaia.financecontroller.identity.application.port.out.MailMessage;
import com.dowglasmaia.financecontroller.identity.application.port.out.MailSender;
import com.dowglasmaia.financecontroller.identity.infrastructure.security.PasswordResetProperties;
import jakarta.mail.internet.MimeMessage;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Component;

/**
 * Envio por SMTP (Spring Mail, configurado por {@code FC_MAIL_*}). Tolerante a falhas: uma nova tentativa e, se falhar
 * de novo, só um registo com identificador interno (nunca o destinatário, o assunto ou o conteúdo). Quem chama não vê erro.
 */
@Component
class SmtpMailSender implements MailSender {

    private static final Logger log = LoggerFactory.getLogger(SmtpMailSender.class);
    private static final int ATTEMPTS = 2;
    private static final long RETRY_DELAY_MS = 500;

    private final JavaMailSender sender;
    private final String from;

    SmtpMailSender(JavaMailSender sender, PasswordResetProperties properties) {
        this.sender = sender;
        this.from = properties.mailFrom();
    }

    @Override
    public void send(MailMessage message) {
        for (int attempt = 1; attempt <= ATTEMPTS; attempt++) {
            try {
                sender.send(mime(message));
                return;
            } catch (Exception failure) {
                if (attempt == ATTEMPTS) {
                    log.warn("mail not sent ref={} cause={}", UUID.randomUUID(), failure.getClass().getSimpleName());
                    return;
                }
                pause();
            }
        }
    }

    private MimeMessage mime(MailMessage message) throws Exception {
        MimeMessage mime = sender.createMimeMessage();
        var helper = new MimeMessageHelper(mime, true, "UTF-8");
        helper.setFrom(from);
        helper.setTo(message.to());
        helper.setSubject(message.subject());
        helper.setText(message.text(), message.html());
        return mime;
    }

    private static void pause() {
        try {
            Thread.sleep(RETRY_DELAY_MS);
        } catch (InterruptedException interrupted) {
            Thread.currentThread().interrupt();
        }
    }
}
