package com.dowglasmaia.financecontroller.identity.application.usecase;

import com.dowglasmaia.financecontroller.identity.application.port.out.AccessTokenIssuer;
import com.dowglasmaia.financecontroller.identity.application.port.out.LoginAttemptLimiter;
import com.dowglasmaia.financecontroller.identity.application.port.out.PasswordHasher;
import com.dowglasmaia.financecontroller.identity.application.port.out.UserRepository;
import com.dowglasmaia.financecontroller.identity.domain.Email;
import com.dowglasmaia.financecontroller.identity.domain.User;
import com.dowglasmaia.financecontroller.shared.error.DomainException;
import com.dowglasmaia.financecontroller.shared.error.ErrorCode;
import java.util.Locale;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class LoginUseCase {

    private final UserRepository users;
    private final PasswordHasher hasher;
    private final AccessTokenIssuer accessTokens;
    private final RefreshSessionIssuer refreshSessions;
    private final LoginAttemptLimiter limiter;

    LoginUseCase(UserRepository users, PasswordHasher hasher, AccessTokenIssuer accessTokens,
                 RefreshSessionIssuer refreshSessions, LoginAttemptLimiter limiter) {
        this.users = users;
        this.hasher = hasher;
        this.accessTokens = accessTokens;
        this.refreshSessions = refreshSessions;
        this.limiter = limiter;
    }

    @Transactional(noRollbackFor = DomainException.class)
    public AuthResult execute(LoginCommand command) {
        var accountKey = command.email() == null ? "" : command.email().strip().toLowerCase(Locale.ROOT);
        var origin = command.origin();
        if (limiter.isBlocked(accountKey, origin)) {
            throw new DomainException(ErrorCode.TOO_MANY_ATTEMPTS);
        }
        var user = findUser(accountKey);
        if (user == null || command.password() == null || !hasher.matches(command.password(), user.passwordHash())) {
            limiter.recordFailure(accountKey, origin);
            throw new DomainException(ErrorCode.INVALID_CREDENTIALS);
        }
        limiter.reset(accountKey, origin);
        var refresh = refreshSessions.openNewFamily(user.id());
        return new AuthResult(user, accessTokens.issue(user), refresh.token(), refresh.expiresAt());
    }

    private User findUser(String accountKey) {
        try {
            return users.findByEmail(Email.of(accountKey)).orElse(null);
        } catch (DomainException invalidEmail) {
            return null;
        }
    }
}
