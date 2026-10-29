package com.dowglasmaia.financecontroller.identity.api;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.dowglasmaia.financecontroller.identity.application.usecase.AuthResult;
import com.dowglasmaia.financecontroller.identity.application.usecase.ConfirmPasswordResetUseCase;
import com.dowglasmaia.financecontroller.identity.application.usecase.GetCurrentUserUseCase;
import com.dowglasmaia.financecontroller.identity.application.usecase.LoginCommand;
import com.dowglasmaia.financecontroller.identity.application.usecase.LoginUseCase;
import com.dowglasmaia.financecontroller.identity.application.usecase.LogoutUseCase;
import com.dowglasmaia.financecontroller.identity.application.usecase.RefreshTokenUseCase;
import com.dowglasmaia.financecontroller.identity.application.usecase.RegisterUserCommand;
import com.dowglasmaia.financecontroller.identity.application.usecase.RegisterUserUseCase;
import com.dowglasmaia.financecontroller.identity.application.usecase.RequestPasswordResetUseCase;
import com.dowglasmaia.financecontroller.identity.application.usecase.TokenRefreshResult;
import com.dowglasmaia.financecontroller.identity.domain.Email;
import com.dowglasmaia.financecontroller.identity.domain.User;
import com.dowglasmaia.financecontroller.identity.domain.UserId;
import com.dowglasmaia.financecontroller.shared.error.ConstraintErrorRegistry;
import com.dowglasmaia.financecontroller.shared.error.DomainException;
import com.dowglasmaia.financecontroller.shared.error.ErrorCode;
import com.dowglasmaia.financecontroller.shared.error.ErrorHandlingConfiguration;
import com.dowglasmaia.financecontroller.shared.error.GlobalExceptionHandler;
import jakarta.servlet.http.Cookie;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.support.StaticListableBeanFactory;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;

/** Standalone MockMvc with mocked use cases: no Spring context, no Docker. */
class IdentityControllerTest {
    private static final Instant NOW = Instant.parse("2026-01-01T00:00:00Z");
    private static final Instant REFRESH_EXPIRES_AT = NOW.plusSeconds(30L * 24 * 3600);
    private static final long REFRESH_MAX_AGE = 30L * 24 * 3600;
    private static final String PASSWORD = "S3nh@-Muito-Secreta";
    private static final String ACCESS_TOKEN = "access.jwt.value";
    private static final String REFRESH_TOKEN = "refresh-opaque-value";
    private static final User USER = new User(UserId.generate(), Email.of("Ana@Example.com"), "$2a$hash-never-exposed", NOW);

    private final RegisterUserUseCase registerUser = mock(RegisterUserUseCase.class);
    private final LoginUseCase login = mock(LoginUseCase.class);
    private final RefreshTokenUseCase refresh = mock(RefreshTokenUseCase.class);
    private final LogoutUseCase logout = mock(LogoutUseCase.class);
    private final GetCurrentUserUseCase currentUser = mock(GetCurrentUserUseCase.class);
    private final RequestPasswordResetUseCase requestReset = mock(RequestPasswordResetUseCase.class);
    private final ConfirmPasswordResetUseCase confirmReset = mock(ConfirmPasswordResetUseCase.class);
    private MockMvc mvc;

    @BeforeEach
    void setUp() {
        mvc = mvcWith(new AuthCookieProperties("refresh_token", true, "Lax", "/api/v1/auth"));
    }

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    private MockMvc mvcWith(AuthCookieProperties cookieProperties) {
        var clock = Clock.fixed(NOW, ZoneOffset.UTC);
        var clockProvider = new StaticListableBeanFactory(Map.of("clock", clock)).getBeanProvider(Clock.class);
        var controller = new IdentityController(registerUser, login, refresh, logout, currentUser, requestReset,
            confirmReset, cookieProperties, clockProvider);
        var messages = new ErrorHandlingConfiguration().messageSource();
        var validator = new LocalValidatorFactoryBean();
        validator.setValidationMessageSource(messages);
        validator.afterPropertiesSet();
        return MockMvcBuilders.standaloneSetup(controller)
            .setControllerAdvice(new GlobalExceptionHandler(messages, new ConstraintErrorRegistry(List.of())))
            .setValidator(validator)
            .build();
    }

    private static MockHttpServletRequestBuilder json(MockHttpServletRequestBuilder request, String body) {
        return request.contentType(MediaType.APPLICATION_JSON).content(body);
    }

    private static String credentials(String email, String password) {
        return "{\"email\":\"" + email + "\",\"password\":\"" + password + "\"}";
    }

    private static void assertRefreshCookie(String setCookie, String value, long maxAge) {
        assertTrue(setCookie.startsWith("refresh_token=" + value + ";"), setCookie);
        assertTrue(setCookie.contains("; HttpOnly"), setCookie);
        assertTrue(setCookie.contains("; Secure"), setCookie);
        assertTrue(setCookie.contains("; SameSite=Lax"), setCookie);
        assertTrue(setCookie.contains("; Path=/api/v1/auth"), setCookie);
        assertTrue(setCookie.contains("Max-Age=" + maxAge), setCookie);
    }

    private static String setCookie(ResultActions result) {
        return result.andReturn().getResponse().getHeader("Set-Cookie");
    }

    @Test
    void passwordResetRequestAnswers202WithTheNeutralMessageAndHandsOverOnlyTheEmailAndOrigin() throws Exception {
        mvc.perform(json(post("/api/v1/auth/password-reset/request"), "{\"email\":\"Ana@Example.com\"}"))
            .andExpect(status().isAccepted())
            .andExpect(jsonPath("$.message").value("Se o e-mail estiver cadastrado, enviaremos um link para redefinir a senha."));

        verify(requestReset).execute(eq("Ana@Example.com"), any());
    }

    @Test
    void passwordResetRequestWithAnInvalidEmailIs400AndTheUseCaseIsNeverReached() throws Exception {
        mvc.perform(json(post("/api/v1/auth/password-reset/request"), "{\"email\":\"nao-e-email\"}"))
            .andExpect(status().isBadRequest());

        verifyNoInteractions(requestReset);
    }

    @Test
    void passwordResetConfirmAnswers204WithoutBodyOrCookies() throws Exception {
        var result = mvc.perform(json(post("/api/v1/auth/password-reset/confirm"),
                "{\"token\":\"abc\",\"newPassword\":\"N0va-Senha-Segura-2026\"}"))
            .andExpect(status().isNoContent());

        verify(confirmReset).execute("abc", "N0va-Senha-Segura-2026");
        assertFalse(result.andReturn().getResponse().containsHeader("Set-Cookie"));
    }

    @Test
    void passwordResetConfirmMapsTheInvalidTokenErrorTo400WithItsCode() throws Exception {
        org.mockito.Mockito.doThrow(new DomainException(ErrorCode.PASSWORD_RESET_TOKEN_INVALID))
            .when(confirmReset).execute(any(), any());

        mvc.perform(json(post("/api/v1/auth/password-reset/confirm"),
                "{\"token\":\"abc\",\"newPassword\":\"N0va-Senha-Segura-2026\"}"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.code").value("PASSWORD_RESET_TOKEN_INVALID"));
    }

    @Test
    void registerAnswers201WithUserAccessTokenAndRefreshCookie() throws Exception {
        when(registerUser.execute(new RegisterUserCommand("Ana@Example.com", PASSWORD)))
            .thenReturn(new AuthResult(USER, ACCESS_TOKEN, REFRESH_TOKEN, REFRESH_EXPIRES_AT));

        var result = mvc.perform(json(post("/api/v1/auth/register"), credentials("Ana@Example.com", PASSWORD)))
            .andExpect(status().isCreated())
            .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$.accessToken").value(ACCESS_TOKEN))
            .andExpect(jsonPath("$.user.id").value(USER.id().value().toString()))
            .andExpect(jsonPath("$.user.email").value("ana@example.com"));

        assertRefreshCookie(setCookie(result), REFRESH_TOKEN, REFRESH_MAX_AGE);
        String body = result.andReturn().getResponse().getContentAsString();
        assertFalse(body.contains(REFRESH_TOKEN), "refresh token must travel only in the cookie");
        assertFalse(body.contains(PASSWORD));
        assertFalse(body.toLowerCase().contains("hash"));
    }

    @Test
    void loginAnswers200WithRefreshCookieAndUsesTheConnectionAddressAsOrigin() throws Exception {
        when(login.execute(new LoginCommand("ana@example.com", PASSWORD, "10.1.2.3")))
            .thenReturn(new AuthResult(USER, ACCESS_TOKEN, REFRESH_TOKEN, REFRESH_EXPIRES_AT));

        var result = mvc.perform(json(post("/api/v1/auth/login"), credentials("ana@example.com", PASSWORD))
                .with(request -> {
                    request.setRemoteAddr("10.1.2.3");
                    return request;
                })
                .header("X-Forwarded-For", "9.9.9.9"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.accessToken").value(ACCESS_TOKEN))
            .andExpect(jsonPath("$.user.email").value("ana@example.com"))
            .andExpect(jsonPath("$.refreshToken").doesNotExist());

        assertRefreshCookie(setCookie(result), REFRESH_TOKEN, REFRESH_MAX_AGE);
        verify(login).execute(new LoginCommand("ana@example.com", PASSWORD, "10.1.2.3"));
    }

    @Test
    void refreshRotatesTheCookieAndReturnsOnlyTheAccessToken() throws Exception {
        when(refresh.execute("old-refresh"))
            .thenReturn(new TokenRefreshResult("new.access.jwt", "new-refresh", REFRESH_EXPIRES_AT));

        var result = mvc.perform(post("/api/v1/auth/refresh").cookie(new Cookie("refresh_token", "old-refresh")))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.accessToken").value("new.access.jwt"))
            .andExpect(jsonPath("$.user").doesNotExist())
            .andExpect(jsonPath("$.refreshToken").doesNotExist());

        assertRefreshCookie(setCookie(result), "new-refresh", REFRESH_MAX_AGE);
        assertFalse(result.andReturn().getResponse().getContentAsString().contains("new-refresh"));
    }

    @Test
    void refreshWithoutCookieIs401WithoutCallingTheUseCase() throws Exception {
        mvc.perform(post("/api/v1/auth/refresh"))
            .andExpect(status().isUnauthorized())
            .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
            .andExpect(jsonPath("$.code").value("UNAUTHENTICATED"));

        verifyNoInteractions(refresh);
    }

    @Test
    void logoutAnswers204AndExpiresTheCookie() throws Exception {
        var result = mvc.perform(post("/api/v1/auth/logout").cookie(new Cookie("refresh_token", REFRESH_TOKEN)))
            .andExpect(status().isNoContent())
            .andExpect(content().string(""));

        verify(logout).execute(REFRESH_TOKEN);
        String setCookie = setCookie(result);
        assertTrue(setCookie.startsWith("refresh_token=;"), setCookie);
        assertTrue(setCookie.contains("Max-Age=0"), setCookie);
        assertTrue(setCookie.contains("; HttpOnly"), setCookie);
        assertTrue(setCookie.contains("; Secure"), setCookie);
        assertTrue(setCookie.contains("; SameSite=Lax"), setCookie);
        assertTrue(setCookie.contains("; Path=/api/v1/auth"), setCookie);
        assertTrue(setCookie.contains("Expires="), setCookie);
    }

    @Test
    void logoutWithoutCookieIsIdempotentAndStillClearsTheCookie() throws Exception {
        var result = mvc.perform(post("/api/v1/auth/logout")).andExpect(status().isNoContent());

        assertTrue(setCookie(result).contains("Max-Age=0"), setCookie(result));
        verify(logout, never()).execute(any());
    }

    @Test
    void meReturnsTheUserOfTheTokenSubjectNeverOfAParameter() throws Exception {
        var other = UserId.generate();
        SecurityContextHolder.getContext().setAuthentication(
            UsernamePasswordAuthenticationToken.authenticated(USER.id().value().toString(), null, List.of()));
        when(currentUser.execute(USER.id())).thenReturn(USER);

        mvc.perform(get("/api/v1/auth/me").param("id", other.value().toString()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(USER.id().value().toString()))
            .andExpect(jsonPath("$.email").value("ana@example.com"))
            .andExpect(jsonPath("$.passwordHash").doesNotExist())
            .andExpect(jsonPath("$.password").doesNotExist());

        verify(currentUser).execute(USER.id());
        verify(currentUser, never()).execute(other);
    }

    @Test
    void meWithoutAuthenticationIsUnauthenticated() throws Exception {
        mvc.perform(get("/api/v1/auth/me"))
            .andExpect(status().isUnauthorized())
            .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
            .andExpect(jsonPath("$.code").value("UNAUTHENTICATED"));

        verifyNoInteractions(currentUser);
    }

    @Test
    void meWithANonUuidSubjectIsUnauthenticated() throws Exception {
        SecurityContextHolder.getContext().setAuthentication(
            UsernamePasswordAuthenticationToken.authenticated("not-a-uuid", null, List.of()));

        mvc.perform(get("/api/v1/auth/me"))
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.code").value("UNAUTHENTICATED"));

        verifyNoInteractions(currentUser);
    }

    @Test
    void cookieAttributesComeFromConfiguration() throws Exception {
        var custom = mvcWith(new AuthCookieProperties("refresh_token", false, "Strict", "/auth"));
        when(login.execute(any())).thenReturn(new AuthResult(USER, ACCESS_TOKEN, REFRESH_TOKEN, REFRESH_EXPIRES_AT));

        String setCookie = setCookie(custom.perform(json(post("/api/v1/auth/login"), credentials("ana@example.com", PASSWORD)))
            .andExpect(status().isOk()));

        assertTrue(setCookie.contains("; HttpOnly"), setCookie);
        assertFalse(setCookie.contains("; Secure"), setCookie);
        assertTrue(setCookie.contains("; SameSite=Strict"), setCookie);
        assertTrue(setCookie.contains("; Path=/auth"), setCookie);
    }

    @Test
    void expiredRefreshTimestampNeverProducesANegativeMaxAge() throws Exception {
        when(login.execute(any())).thenReturn(new AuthResult(USER, ACCESS_TOKEN, REFRESH_TOKEN, NOW.minusSeconds(60)));

        String setCookie = setCookie(mvc.perform(json(post("/api/v1/auth/login"), credentials("ana@example.com", PASSWORD)))
            .andExpect(status().isOk()));

        assertTrue(setCookie.contains("Max-Age=0"), setCookie);
    }

    @Test
    void invalidCredentialsErrorBodyLeaksNoSecretsAndSetsNoCookie() throws Exception {
        when(login.execute(any())).thenThrow(new DomainException(ErrorCode.INVALID_CREDENTIALS));

        var result = mvc.perform(json(post("/api/v1/auth/login"), credentials("ana@example.com", PASSWORD)))
            .andExpect(status().isUnauthorized())
            .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
            .andExpect(jsonPath("$.code").value("INVALID_CREDENTIALS"))
            .andExpect(header().doesNotExist("Set-Cookie"));

        assertNoSecrets(result.andReturn().getResponse().getContentAsString());
    }

    @Test
    void duplicatedEmailIs409WithoutSecrets() throws Exception {
        when(registerUser.execute(any())).thenThrow(new DomainException(ErrorCode.EMAIL_ALREADY_REGISTERED));

        var result = mvc.perform(json(post("/api/v1/auth/register"), credentials("ana@example.com", PASSWORD)))
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.code").value("EMAIL_ALREADY_REGISTERED"))
            .andExpect(header().doesNotExist("Set-Cookie"));

        assertNoSecrets(result.andReturn().getResponse().getContentAsString());
    }

    @Test
    void tooManyAttemptsIs429WithoutSecrets() throws Exception {
        when(login.execute(any())).thenThrow(new DomainException(ErrorCode.TOO_MANY_ATTEMPTS));

        var result = mvc.perform(json(post("/api/v1/auth/login"), credentials("ana@example.com", PASSWORD)))
            .andExpect(status().isTooManyRequests())
            .andExpect(jsonPath("$.code").value("TOO_MANY_ATTEMPTS"));

        assertNoSecrets(result.andReturn().getResponse().getContentAsString());
    }

    @Test
    void invalidRegistrationBodyIs400AndNeverEchoesThePassword() throws Exception {
        var result = mvc.perform(json(post("/api/v1/auth/register"), credentials("not-an-email", "short")))
            .andExpect(status().isBadRequest())
            .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
            .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"));

        assertFalse(result.andReturn().getResponse().getContentAsString().contains("short"));
        verifyNoInteractions(registerUser);
    }

    @Test
    void passwordAboveTheMaximumLengthIsRejectedBeforeTheUseCase() throws Exception {
        String tooLong = "a".repeat(129);

        mvc.perform(json(post("/api/v1/auth/login"), credentials("ana@example.com", tooLong)))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"));

        verifyNoInteractions(login);
    }

    private static void assertNoSecrets(String body) {
        assertFalse(body.isBlank());
        for (String forbidden : List.of(PASSWORD, "hash", "accessToken", "refresh_token", "refreshToken", ACCESS_TOKEN,
                REFRESH_TOKEN, "Bearer")) {
            assertFalse(body.contains(forbidden), "error body leaks '" + forbidden + "': " + body);
        }
    }
}
