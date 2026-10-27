package com.dowglasmaia.financecontroller.identity.infrastructure.security;

import com.dowglasmaia.financecontroller.identity.CurrentOwner;
import com.dowglasmaia.financecontroller.shared.error.DomainException;
import com.dowglasmaia.financecontroller.shared.error.ErrorCode;

import java.util.Objects;
import java.util.UUID;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;

/** Proprietário autenticado = {@code sub} do JWT já validado pelo filtro de segurança. */
@Component
class SecurityContextCurrentOwner implements CurrentOwner {

    @Override
    public UUID id() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication instanceof JwtAuthenticationToken token && token.isAuthenticated()) {
            try {
                return UUID.fromString(Objects.requireNonNull(token.getToken().getSubject()));
            } catch (IllegalArgumentException | NullPointerException e) {
                throw new DomainException(ErrorCode.UNAUTHENTICATED);
            }
        }
        throw new DomainException(ErrorCode.UNAUTHENTICATED);
    }
}
