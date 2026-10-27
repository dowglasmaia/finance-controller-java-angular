package com.dowglasmaia.financecontroller.identity.api;

import com.dowglasmaia.financecontroller.generated.identity.api.IdentityApi;
import com.dowglasmaia.financecontroller.generated.identity.model.AccessTokenResponse;
import com.dowglasmaia.financecontroller.generated.identity.model.AuthResponse;
import com.dowglasmaia.financecontroller.generated.identity.model.LoginRequest;
import com.dowglasmaia.financecontroller.generated.identity.model.RegisterRequest;
import com.dowglasmaia.financecontroller.generated.identity.model.UserResponse;
import com.dowglasmaia.financecontroller.identity.application.usecase.AuthResult;
import com.dowglasmaia.financecontroller.identity.application.usecase.GetCurrentUserUseCase;
import com.dowglasmaia.financecontroller.identity.application.usecase.LoginUseCase;
import com.dowglasmaia.financecontroller.identity.application.usecase.LogoutUseCase;
import com.dowglasmaia.financecontroller.identity.application.usecase.RefreshTokenUseCase;
import com.dowglasmaia.financecontroller.identity.application.usecase.RegisterUserUseCase;
import com.dowglasmaia.financecontroller.identity.application.usecase.TokenRefreshResult;
import com.dowglasmaia.financecontroller.identity.domain.UserId;
import com.dowglasmaia.financecontroller.shared.error.DomainException;
import com.dowglasmaia.financecontroller.shared.error.ErrorCode;
import jakarta.servlet.http.HttpServletRequest;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

/** Só mapeia e delega; as regras vivem nos casos de uso. */
@RestController
public class IdentityController implements IdentityApi {
    private final RegisterUserUseCase registerUser;
    private final LoginUseCase login;
    private final RefreshTokenUseCase refreshToken;
    private final LogoutUseCase logout;
    private final GetCurrentUserUseCase getCurrentUser;
    private final AuthCookieProperties cookie;
    private final Clock clock;

    public IdentityController(RegisterUserUseCase registerUser, LoginUseCase login, RefreshTokenUseCase refreshToken,
            LogoutUseCase logout, GetCurrentUserUseCase getCurrentUser, AuthCookieProperties cookie,
            ObjectProvider<Clock> clock) {
        this.registerUser = registerUser;
        this.login = login;
        this.refreshToken = refreshToken;
        this.logout = logout;
        this.getCurrentUser = getCurrentUser;
        this.cookie = cookie;
        this.clock = clock.getIfAvailable(Clock::systemUTC);
    }

    @Override
    public ResponseEntity<AuthResponse> registerUser(RegisterRequest registerRequest) {
        AuthResult result = registerUser.execute(IdentityMapper.toCommand(registerRequest));
        return ResponseEntity.status(HttpStatus.CREATED)
            .header(HttpHeaders.SET_COOKIE, refreshCookie(result.refreshToken(), result.refreshTokenExpiresAt()))
            .body(IdentityMapper.toResponse(result));
    }

    @Override
    public ResponseEntity<AuthResponse> login(LoginRequest loginRequest) {
        // Endereço da conexão; X-Forwarded-For é forjável e não é usado.
        AuthResult result = login.execute(IdentityMapper.toCommand(loginRequest, currentRequest().getRemoteAddr()));
        return ResponseEntity.ok()
            .header(HttpHeaders.SET_COOKIE, refreshCookie(result.refreshToken(), result.refreshTokenExpiresAt()))
            .body(IdentityMapper.toResponse(result));
    }

    @Override
    public ResponseEntity<AccessTokenResponse> refreshToken(String refreshToken) {
        if (refreshToken == null || refreshToken.isBlank()) {
            throw new DomainException(ErrorCode.UNAUTHENTICATED);
        }
        TokenRefreshResult result = this.refreshToken.execute(refreshToken);
        return ResponseEntity.ok()
            .header(HttpHeaders.SET_COOKIE, refreshCookie(result.refreshToken(), result.refreshTokenExpiresAt()))
            .body(IdentityMapper.toResponse(result));
    }

    @Override
    public ResponseEntity<Void> logout(String refreshToken) {
        if (refreshToken != null && !refreshToken.isBlank()) {
            logout.execute(refreshToken);
        }
        return ResponseEntity.noContent().header(HttpHeaders.SET_COOKIE, expiredCookie()).build();
    }

    @Override
    public ResponseEntity<UserResponse> getCurrentUser() {
        return ResponseEntity.ok(IdentityMapper.toResponse(getCurrentUser.execute(authenticatedUserId())));
    }

    private String refreshCookie(String value, Instant expiresAt) {
        Duration maxAge = Duration.between(clock.instant(), expiresAt);
        return baseCookie(value).maxAge(maxAge.isNegative() ? Duration.ZERO : maxAge).build().toString();
    }

    private String expiredCookie() {
        return baseCookie("").maxAge(Duration.ZERO).build().toString();
    }

    private ResponseCookie.ResponseCookieBuilder baseCookie(String value) {
        return ResponseCookie.from(cookie.name(), value)
            .httpOnly(true)
            .secure(cookie.secure())
            .sameSite(cookie.sameSite())
            .path(cookie.path());
    }

    /** O proprietário vem sempre do token (sujeito do JWT já validado), nunca de parâmetro. */
    private static UserId authenticatedUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()
                || authentication instanceof AnonymousAuthenticationToken) {
            throw new DomainException(ErrorCode.UNAUTHENTICATED);
        }
        try {
            return UserId.of(authentication.getName());
        } catch (IllegalArgumentException invalidSubject) {
            throw new DomainException(ErrorCode.UNAUTHENTICATED);
        }
    }

    private static HttpServletRequest currentRequest() {
        return ((ServletRequestAttributes) RequestContextHolder.currentRequestAttributes()).getRequest();
    }
}
