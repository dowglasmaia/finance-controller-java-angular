package com.dowglasmaia.financecontroller.identity;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.dowglasmaia.financecontroller.support.AbstractPostgresIntegrationTest;
import com.jayway.jsonpath.JsonPath;
import jakarta.servlet.http.Cookie;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

/**
 * Full identity flow against the real application context and a real PostgreSQL (Testcontainers): registration,
 * access token, refresh rotation and reuse detection, logout, throttling and isolation between users.
 * Every test uses its own e-mail and its own remote address so the throttling state of one test never leaks into another.
 */
@AutoConfigureMockMvc
class IdentityFlowIntegrationTest extends AbstractPostgresIntegrationTest {
    private static final String PASSWORD = "Sup3r-Secret-Passw0rd";
    private static final String WRONG_PASSWORD = "Wrong-Passw0rd-123";
    private static final int MAX_ATTEMPTS_PROBE = 60;

    /** Dummy signing secret (at least 32 bytes) for tests only. */
    @DynamicPropertySource
    static void identityProperties(DynamicPropertyRegistry registry) {
        registry.add("app.identity.jwt.secret", () -> "test-only-dummy-jwt-secret-0123456789-abcdef");
        // o perfil "local" desliga o Secure por conveniência; o teste verifica o comportamento de produção
        registry.add("app.identity.cookie.secure", () -> "true");
    }

    @Autowired
    private MockMvc mvc;

    private static String uniqueEmail(String prefix) {
        return prefix + "." + UUID.randomUUID() + "@Example.com";
    }

    private static String credentials(String email, String password) {
        return "{\"email\":\"" + email + "\",\"password\":\"" + password + "\"}";
    }

    private static MockHttpServletRequestBuilder from(MockHttpServletRequestBuilder request, String remoteAddress) {
        return request.with(r -> {
            r.setRemoteAddr(remoteAddress);
            return r;
        });
    }

    private static MockHttpServletRequestBuilder json(MockHttpServletRequestBuilder request, String body) {
        return request.contentType(MediaType.APPLICATION_JSON).content(body);
    }

    private MvcResult register(String email, String remoteAddress) throws Exception {
        return mvc.perform(from(json(post("/api/v1/auth/register"), credentials(email, PASSWORD)), remoteAddress))
            .andReturn();
    }

    private MvcResult login(String email, String password, String remoteAddress) throws Exception {
        return mvc.perform(from(json(post("/api/v1/auth/login"), credentials(email, password)), remoteAddress))
            .andReturn();
    }

    private MvcResult refresh(String refreshToken) throws Exception {
        return mvc.perform(post("/api/v1/auth/refresh").cookie(new Cookie("refresh_token", refreshToken))).andReturn();
    }

    private MvcResult me(String accessToken) throws Exception {
        return mvc.perform(get("/api/v1/auth/me").header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)).andReturn();
    }

    private static String accessToken(MvcResult result) throws Exception {
        return field(result, "$.accessToken");
    }

    private static String refreshToken(MvcResult result) {
        String setCookie = result.getResponse().getHeader(HttpHeaders.SET_COOKIE);
        assertTrue(setCookie != null && setCookie.startsWith("refresh_token="), "missing refresh cookie: " + setCookie);
        return setCookie.substring("refresh_token=".length(), setCookie.indexOf(';'));
    }

    private static String field(MvcResult result, String path) throws Exception {
        Object value = JsonPath.read(body(result), path);
        return String.valueOf(value);
    }

    private static String body(MvcResult result) throws Exception {
        return result.getResponse().getContentAsString();
    }

    private static void assertProblem(MvcResult result, int status, String code) throws Exception {
        assertEquals(status, result.getResponse().getStatus(), body(result));
        assertTrue(MediaType.APPLICATION_PROBLEM_JSON.isCompatibleWith(MediaType.parseMediaType(result.getResponse().getContentType())),
            result.getResponse().getContentType());
        assertEquals(code, field(result, "$.code"));
    }

    private static void assertNoSecrets(MvcResult result, String... extraForbidden) throws Exception {
        // the access token is a legitimate field of success bodies; it is random base64 and must not trip the "hash" scan
        String body = body(result).replaceAll("\"accessToken\":\"[^\"]*\"", "");
        assertFalse(body.contains(PASSWORD), "response leaks the password");
        assertFalse(body.contains(WRONG_PASSWORD), "response leaks the submitted password");
        assertFalse(body.toLowerCase().contains("hash"), "response leaks a hash: " + body);
        assertFalse(body.contains("$2"), "response leaks a bcrypt hash: " + body);
        assertFalse(body.contains("refresh_token"), "response leaks a refresh token: " + body);
        for (String forbidden : extraForbidden) {
            assertFalse(body.contains(forbidden), "response leaks '" + forbidden + "'");
        }
    }

    @Test
    void fullFlowRegisterMeLoginRefreshRotationReuseAndLogout() throws Exception {
        String email = uniqueEmail("flow");
        String address = "10.20.0.1";

        // register
        MvcResult registered = register(email, address);
        assertEquals(201, registered.getResponse().getStatus(), body(registered));
        String registeredAccess = accessToken(registered);
        String registeredRefresh = refreshToken(registered);
        String cookieHeader = registered.getResponse().getHeader(HttpHeaders.SET_COOKIE);
        assertTrue(cookieHeader.contains("HttpOnly"), cookieHeader);
        assertTrue(cookieHeader.contains("Secure"), cookieHeader);
        assertTrue(cookieHeader.contains("SameSite=Lax"), cookieHeader);
        assertTrue(cookieHeader.contains("Path=/api/v1/auth"), cookieHeader);
        assertTrue(cookieHeader.contains("Max-Age="), cookieHeader);
        assertNoSecrets(registered, registeredRefresh);
        assertEquals(email.toLowerCase(), field(registered, "$.user.email"));

        // me with the access token
        MvcResult current = me(registeredAccess);
        assertEquals(200, current.getResponse().getStatus(), body(current));
        assertEquals(email.toLowerCase(), field(current, "$.email"));
        assertEquals(field(registered, "$.user.id"), field(current, "$.id"));
        assertNoSecrets(current, registeredAccess);

        // login
        MvcResult loggedIn = login(email, PASSWORD, address);
        assertEquals(200, loggedIn.getResponse().getStatus(), body(loggedIn));
        assertEquals(email.toLowerCase(), field(loggedIn, "$.user.email"));
        String loginRefresh = refreshToken(loggedIn);
        assertNotEquals(registeredRefresh, loginRefresh);
        assertNoSecrets(loggedIn, loginRefresh);

        // refresh rotates the cookie and issues a usable access token
        MvcResult refreshed = refresh(loginRefresh);
        assertEquals(200, refreshed.getResponse().getStatus(), body(refreshed));
        String rotatedRefresh = refreshToken(refreshed);
        assertNotEquals(loginRefresh, rotatedRefresh);
        assertNoSecrets(refreshed, rotatedRefresh);
        MvcResult afterRefresh = me(accessToken(refreshed));
        assertEquals(200, afterRefresh.getResponse().getStatus(), body(afterRefresh));

        // reusing the old refresh token is refused and revokes the whole family
        MvcResult reuse = refresh(loginRefresh);
        assertProblem(reuse, 401, "UNAUTHENTICATED");
        assertNoSecrets(reuse, loginRefresh, rotatedRefresh);
        MvcResult newerAlsoRevoked = refresh(rotatedRefresh);
        assertProblem(newerAlsoRevoked, 401, "UNAUTHENTICATED");

        // another family (the registration session) is untouched by that revocation; logout revokes it
        MvcResult stillValid = refresh(registeredRefresh);
        assertEquals(200, stillValid.getResponse().getStatus(), body(stillValid));
        String lastRefresh = refreshToken(stillValid);
        MvcResult loggedOut = mvc.perform(post("/api/v1/auth/logout").cookie(new Cookie("refresh_token", lastRefresh)))
            .andExpect(status().isNoContent())
            .andReturn();
        String expired = loggedOut.getResponse().getHeader(HttpHeaders.SET_COOKIE);
        assertTrue(expired.startsWith("refresh_token=;"), expired);
        assertTrue(expired.contains("Max-Age=0"), expired);
        assertProblem(refresh(lastRefresh), 401, "UNAUTHENTICATED");
    }

    @Test
    void duplicatedEmailInAnotherCaseIs409() throws Exception {
        String local = "dup." + UUID.randomUUID();
        String address = "10.20.0.2";
        assertEquals(201, register(local + "@example.com", address).getResponse().getStatus());

        MvcResult duplicated = register((local + "@EXAMPLE.COM").replace("dup", "DUP"), address);

        assertProblem(duplicated, 409, "EMAIL_ALREADY_REGISTERED");
        assertNoSecrets(duplicated);
    }

    @Test
    void unknownEmailAndWrongPasswordGiveTheSameInvalidCredentialsResponse() throws Exception {
        String email = uniqueEmail("creds");
        assertEquals(201, register(email, "10.20.0.3").getResponse().getStatus());

        MvcResult wrongPassword = login(email, WRONG_PASSWORD, "10.20.0.3");
        MvcResult unknownEmail = login(uniqueEmail("ghost"), WRONG_PASSWORD, "10.20.0.4");

        assertProblem(wrongPassword, 401, "INVALID_CREDENTIALS");
        assertProblem(unknownEmail, 401, "INVALID_CREDENTIALS");
        assertEquals(field(wrongPassword, "$.detail"), field(unknownEmail, "$.detail"));
        assertEquals(field(wrongPassword, "$.title"), field(unknownEmail, "$.title"));
        assertEquals(field(wrongPassword, "$.type"), field(unknownEmail, "$.type"));
        assertEquals(wrongPassword.getResponse().getHeader(HttpHeaders.SET_COOKIE),
            unknownEmail.getResponse().getHeader(HttpHeaders.SET_COOKIE));
        assertNoSecrets(wrongPassword);
        assertNoSecrets(unknownEmail);
    }

    @Test
    void repeatedFailuresAreThrottledWith429() throws Exception {
        String email = uniqueEmail("throttle");
        assertEquals(201, register(email, "10.20.0.5").getResponse().getStatus());

        int firstThrottled = -1;
        for (int attempt = 1; attempt <= MAX_ATTEMPTS_PROBE && firstThrottled < 0; attempt++) {
            MvcResult result = login(email, WRONG_PASSWORD, "10.20.0.5");
            if (result.getResponse().getStatus() == 429) {
                firstThrottled = attempt;
                assertProblem(result, 429, "TOO_MANY_ATTEMPTS");
                assertNoSecrets(result);
            } else {
                assertProblem(result, 401, "INVALID_CREDENTIALS");
            }
        }
        assertTrue(firstThrottled > 1, "login was never throttled within " + MAX_ATTEMPTS_PROBE + " failures");

        // the account stays blocked even for the right password and from another origin
        assertProblem(login(email, PASSWORD, "10.20.0.6"), 429, "TOO_MANY_ATTEMPTS");
    }

    @Test
    void protectedRouteWithoutTokenIs401ProblemJson() throws Exception {
        mvc.perform(get("/api/v1/auth/me"))
            .andExpect(status().isUnauthorized())
            .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
            .andExpect(jsonPath("$.code").value("UNAUTHENTICATED"));

        mvc.perform(get("/api/v1/auth/me").header(HttpHeaders.AUTHORIZATION, "Bearer not.a.valid-token"))
            .andExpect(status().isUnauthorized())
            .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
            .andExpect(jsonPath("$.code").value("UNAUTHENTICATED"));
    }

    @Test
    void refreshWithoutCookieOrWithUnknownTokenIs401() throws Exception {
        assertProblem(mvc.perform(post("/api/v1/auth/refresh")).andReturn(), 401, "UNAUTHENTICATED");
        assertProblem(refresh("unknown-" + UUID.randomUUID()), 401, "UNAUTHENTICATED");
    }

    @Test
    void invalidRegistrationIs400WithoutEchoingThePassword() throws Exception {
        MvcResult result = mvc.perform(json(post("/api/v1/auth/register"), credentials("not-an-email", "short"))).andReturn();

        assertProblem(result, 400, "VALIDATION_FAILED");
        assertFalse(body(result).contains("short"));
    }

    @Test
    void eachTokenOnlySeesItsOwnUser() throws Exception {
        String emailA = uniqueEmail("owner.a");
        String emailB = uniqueEmail("owner.b");
        MvcResult userA = register(emailA, "10.20.0.7");
        MvcResult userB = register(emailB, "10.20.0.8");
        assertEquals(201, userA.getResponse().getStatus());
        assertEquals(201, userB.getResponse().getStatus());
        String idA = field(userA, "$.user.id");
        String idB = field(userB, "$.user.id");
        assertNotEquals(idA, idB);

        MvcResult meA = me(accessToken(userA));
        MvcResult meB = me(accessToken(userB));

        assertEquals(200, meA.getResponse().getStatus());
        assertEquals(idA, field(meA, "$.id"));
        assertEquals(emailA.toLowerCase(), field(meA, "$.email"));
        assertFalse(body(meA).contains(idB));
        assertFalse(body(meA).toLowerCase().contains(emailB.toLowerCase()));
        assertEquals(idB, field(meB, "$.id"));
        assertEquals(emailB.toLowerCase(), field(meB, "$.email"));
        assertFalse(body(meB).contains(idA));
        assertFalse(body(meB).toLowerCase().contains(emailA.toLowerCase()));

        // an id sent as a parameter never changes whose data is returned
        MvcResult forged = mvc.perform(get("/api/v1/auth/me").param("id", idB)
            .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken(userA))).andReturn();
        assertEquals(idA, field(forged, "$.id"));

        // refresh and logout of one user never touch the other's session
        String refreshB = refreshToken(userB);
        mvc.perform(post("/api/v1/auth/logout").cookie(new Cookie("refresh_token", refreshToken(userA))))
            .andExpect(status().isNoContent());
        MvcResult stillB = refresh(refreshB);
        assertEquals(200, stillB.getResponse().getStatus(), body(stillB));
        assertNoSecrets(meA, accessToken(userA));
        assertNoSecrets(meB, accessToken(userB));
    }
}
