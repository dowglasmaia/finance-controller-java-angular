package com.dowglasmaia.financecontroller.identity.application.usecase;

import com.dowglasmaia.financecontroller.identity.UserRegistered;
import com.dowglasmaia.financecontroller.identity.application.port.out.AccessTokenIssuer;
import com.dowglasmaia.financecontroller.identity.application.port.out.PasswordHasher;
import com.dowglasmaia.financecontroller.identity.application.port.out.UserRepository;
import com.dowglasmaia.financecontroller.identity.domain.Email;
import com.dowglasmaia.financecontroller.identity.domain.PasswordPolicy;
import com.dowglasmaia.financecontroller.identity.domain.User;
import com.dowglasmaia.financecontroller.identity.domain.UserId;
import com.dowglasmaia.financecontroller.shared.error.DomainException;
import com.dowglasmaia.financecontroller.shared.error.ErrorCode;
import java.time.Clock;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class RegisterUserUseCase {

    private final UserRepository users;
    private final PasswordHasher hasher;
    private final AccessTokenIssuer accessTokens;
    private final RefreshSessionIssuer refreshSessions;
    private final Clock clock;
    private final ApplicationEventPublisher events;

    RegisterUserUseCase(UserRepository users, PasswordHasher hasher, AccessTokenIssuer accessTokens,
                        RefreshSessionIssuer refreshSessions, Clock clock, ApplicationEventPublisher events) {
        this.users = users;
        this.hasher = hasher;
        this.accessTokens = accessTokens;
        this.refreshSessions = refreshSessions;
        this.clock = clock;
        this.events = events;
    }

    @Transactional
    public AuthResult execute(RegisterUserCommand command) {
        var email = Email.of(command.email());
        PasswordPolicy.validate(command.password());
        if (users.existsByEmail(email)) {
            throw new DomainException(ErrorCode.EMAIL_ALREADY_REGISTERED);
        }
        var user = users.save(new User(UserId.generate(), email, hasher.hash(command.password()), clock.instant()));
        events.publishEvent(new UserRegistered(user.id().value()));
        var refresh = refreshSessions.openNewFamily(user.id());
        return new AuthResult(user, accessTokens.issue(user), refresh.token(), refresh.expiresAt());
    }
}
