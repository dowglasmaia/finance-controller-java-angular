package com.dowglasmaia.financecontroller.identity.application.usecase;

import com.dowglasmaia.financecontroller.identity.application.port.out.MailSender;
import com.dowglasmaia.financecontroller.identity.application.port.out.PasswordResetRequestLimiter;
import com.dowglasmaia.financecontroller.identity.application.port.out.PasswordResetSettings;
import com.dowglasmaia.financecontroller.identity.application.port.out.PasswordResetTokenRepository;
import com.dowglasmaia.financecontroller.identity.application.port.out.UserRepository;
import com.dowglasmaia.financecontroller.identity.domain.Email;
import com.dowglasmaia.financecontroller.identity.domain.ResetToken;
import java.time.Clock;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Trabalho de recuperação de senha que roda <b>depois</b> da resposta (em outra thread): procurar a conta, criar o
 * token e enviar o e-mail. Como tudo acontece fora do pedido, o tempo da resposta é o mesmo exista a conta ou não.
 * Qualquer falha é só registada com um identificador interno: nunca com e-mail, token ou senha.
 */
@Service
public class PasswordResetProcessor {

    private static final Logger log = LoggerFactory.getLogger(PasswordResetProcessor.class);

    private final UserRepository users;
    private final PasswordResetTokenRepository tokens;
    private final PasswordResetRequestLimiter limiter;
    private final MailSender mail;
    private final PasswordResetSettings settings;
    private final Clock clock;

    PasswordResetProcessor(UserRepository users, PasswordResetTokenRepository tokens, PasswordResetRequestLimiter limiter,
                           MailSender mail, PasswordResetSettings settings, Clock clock) {
        this.users = users;
        this.tokens = tokens;
        this.limiter = limiter;
        this.mail = mail;
        this.settings = settings;
        this.clock = clock;
    }

    /** Pedido de link: dentro do limite e com conta existente, invalida os anteriores, cria o token e envia o e-mail. */
    @Async("mailTaskExecutor")
    @Transactional
    public void requestLink(Email email, String origin) {
        try {
            if (!limiter.tryAcquire(email.value(), origin)) {
                return;
            }
            var user = users.findByEmail(email).orElse(null);
            if (user == null) {
                return;
            }
            var now = clock.instant();
            tokens.invalidateAllOf(user.id(), now);
            var token = ResetToken.generate();
            tokens.save(user.id(), token.hash(), now.plus(settings.tokenTtl()), now);
            mail.send(PasswordResetEmails.resetLink(email.value(), link(token), settings.tokenTtl()));
        } catch (RuntimeException failure) {
            log.warn("password reset request not processed ref={} cause={}", UUID.randomUUID(), failure.getClass().getSimpleName());
        }
    }

    /** Aviso de senha alterada. */
    @Async("mailTaskExecutor")
    public void notifyChanged(Email email) {
        try {
            mail.send(PasswordResetEmails.passwordChanged(email.value()));
        } catch (RuntimeException failure) {
            log.warn("password changed notice not sent ref={} cause={}", UUID.randomUUID(), failure.getClass().getSimpleName());
        }
    }

    private String link(ResetToken token) {
        String base = settings.appBaseUrl().endsWith("/")
            ? settings.appBaseUrl().substring(0, settings.appBaseUrl().length() - 1) : settings.appBaseUrl();
        return base + "/redefinir-senha?token=" + token.value();
    }
}
