package com.dowglasmaia.financecontroller.identity.application.usecase;

import com.dowglasmaia.financecontroller.identity.application.port.out.PasswordHasher;
import com.dowglasmaia.financecontroller.identity.application.port.out.PasswordResetTokenRepository;
import com.dowglasmaia.financecontroller.identity.application.port.out.RefreshSessionRepository;
import com.dowglasmaia.financecontroller.identity.application.port.out.UserRepository;
import com.dowglasmaia.financecontroller.identity.domain.PasswordPolicy;
import com.dowglasmaia.financecontroller.identity.domain.ResetToken;
import com.dowglasmaia.financecontroller.identity.domain.User;
import com.dowglasmaia.financecontroller.shared.error.DomainException;
import com.dowglasmaia.financecontroller.shared.error.ErrorCode;
import java.time.Clock;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Define a nova senha com o token do e-mail. Token inexistente, vencido, já usado ou alterado dá a mesma resposta
 * ({@code PASSWORD_RESET_TOKEN_INVALID}). O sucesso grava a senha, consome o token, revoga todas as sessões da conta e
 * avisa por e-mail; não autentica automaticamente. Tudo na mesma transação: qualquer falha desfaz tudo, inclusive o
 * consumo do token.
 */
@Service
public class ConfirmPasswordResetUseCase {

    private final PasswordResetTokenRepository tokens;
    private final UserRepository users;
    private final PasswordHasher hasher;
    private final RefreshSessionRepository sessions;
    private final PasswordResetProcessor processor;
    private final Clock clock;

    ConfirmPasswordResetUseCase(PasswordResetTokenRepository tokens, UserRepository users, PasswordHasher hasher,
                                RefreshSessionRepository sessions, PasswordResetProcessor processor, Clock clock) {
        this.tokens = tokens;
        this.users = users;
        this.hasher = hasher;
        this.sessions = sessions;
        this.processor = processor;
        this.clock = clock;
    }

    @Transactional
    public void execute(String token, String newPassword) {
        PasswordPolicy.validate(newPassword);
        if (token == null || token.isBlank() || token.length() > ResetToken.MAX_LENGTH) {
            throw new DomainException(ErrorCode.PASSWORD_RESET_TOKEN_INVALID);
        }
        var now = clock.instant();
        var userId = tokens.consume(ResetToken.hash(token.strip()), now)
            .orElseThrow(() -> new DomainException(ErrorCode.PASSWORD_RESET_TOKEN_INVALID));
        var user = users.findById(userId).orElseThrow(() -> new DomainException(ErrorCode.PASSWORD_RESET_TOKEN_INVALID));
        users.save(new User(user.id(), user.email(), hasher.hash(newPassword), user.createdAt()));
        tokens.invalidateAllOf(user.id(), now);
        sessions.revokeAllOfUser(user.id(), now);
        processor.notifyChanged(user.email());
    }
}
