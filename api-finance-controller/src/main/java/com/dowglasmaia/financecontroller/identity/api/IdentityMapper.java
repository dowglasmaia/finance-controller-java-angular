package com.dowglasmaia.financecontroller.identity.api;

import com.dowglasmaia.financecontroller.generated.identity.model.AccessTokenResponse;
import com.dowglasmaia.financecontroller.generated.identity.model.AuthResponse;
import com.dowglasmaia.financecontroller.generated.identity.model.LoginRequest;
import com.dowglasmaia.financecontroller.generated.identity.model.RegisterRequest;
import com.dowglasmaia.financecontroller.generated.identity.model.UserResponse;
import com.dowglasmaia.financecontroller.identity.application.usecase.AuthResult;
import com.dowglasmaia.financecontroller.identity.application.usecase.LoginCommand;
import com.dowglasmaia.financecontroller.identity.application.usecase.RegisterUserCommand;
import com.dowglasmaia.financecontroller.identity.application.usecase.TokenRefreshResult;
import com.dowglasmaia.financecontroller.identity.domain.User;

/** Mapeamento manual entre DTOs gerados e domínio; nunca expõe o hash da senha. */
final class IdentityMapper {

    private IdentityMapper() {
    }

    static RegisterUserCommand toCommand(RegisterRequest request) {
        return new RegisterUserCommand(request.getEmail(), request.getPassword());
    }

    static LoginCommand toCommand(LoginRequest request, String origin) {
        return new LoginCommand(request.getEmail(), request.getPassword(), origin);
    }

    static UserResponse toResponse(User user) {
        return new UserResponse(user.id().value(), user.email().value());
    }

    static AuthResponse toResponse(AuthResult result) {
        return new AuthResponse(toResponse(result.user()), result.accessToken());
    }

    static AccessTokenResponse toResponse(TokenRefreshResult result) {
        return new AccessTokenResponse(result.accessToken());
    }
}
