package com.dowglasmaia.financecontroller.identity;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

import com.dowglasmaia.financecontroller.identity.application.port.out.PasswordResetTokenRepository;
import com.dowglasmaia.financecontroller.identity.domain.ResetToken;
import com.dowglasmaia.financecontroller.support.AbstractPostgresIntegrationTest;
import com.dowglasmaia.financecontroller.support.MailpitSupport;
import com.jayway.jsonpath.JsonPath;
import jakarta.servlet.http.Cookie;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.awaitility.Awaitility;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

/**
 * Password recovery against a real PostgreSQL and a real SMTP capture server (Mailpit): neutral answers, e-mail content,
 * hashed token, single use, expiry, invalidation by a new request, session revocation, rate limit and concurrent confirms.
 */
@AutoConfigureMockMvc
class PasswordResetFlowIntegrationTest extends AbstractPostgresIntegrationTest {
    private static final String PASSWORD = "Sup3r-Secret-Passw0rd";
    private static final String NEW_PASSWORD = "N0va-Senha-Segura-2026";
    private static final Pattern LINK = Pattern.compile("http://localhost:4200/redefinir-senha\\?token=([A-Za-z0-9_-]+)");

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
        registry.add("app.identity.jwt.secret", () -> "test-only-dummy-jwt-secret-0123456789-abcdef");
        registry.add("spring.mail.host", MailpitSupport::smtpHost);
        registry.add("spring.mail.port", MailpitSupport::smtpPort);
    }

    @Autowired
    private MockMvc mvc;

    @Autowired
    private JdbcTemplate jdbc;

    @Autowired
    private PasswordResetTokenRepository tokenRepository;

    private static String newEmail() {
        return "reset." + UUID.randomUUID() + "@example.com";
    }

    private static String freshOrigin() {
        return "10." + (int) (Math.random() * 250) + "." + (int) (Math.random() * 250) + "." + (int) (Math.random() * 250);
    }

    private MvcResult register(String email) throws Exception {
        MvcResult result = mvc.perform(post("/api/v1/auth/register").with(r -> {
            r.setRemoteAddr(freshOrigin());
            return r;
        }).contentType(MediaType.APPLICATION_JSON)
            .content("{\"email\":\"" + email + "\",\"password\":\"" + PASSWORD + "\"}")).andReturn();
        assertEquals(201, result.getResponse().getStatus());
        return result;
    }

    private MvcResult requestReset(String email) throws Exception {
        return mvc.perform(post("/api/v1/auth/password-reset/request").with(r -> {
            r.setRemoteAddr(freshOrigin());
            return r;
        }).contentType(MediaType.APPLICATION_JSON).content("{\"email\":\"" + email + "\"}")).andReturn();
    }

    private MvcResult confirm(String token, String newPassword) throws Exception {
        return mvc.perform(post("/api/v1/auth/password-reset/confirm").contentType(MediaType.APPLICATION_JSON)
            .content("{\"token\":\"" + token + "\",\"newPassword\":\"" + newPassword + "\"}")).andReturn();
    }

    private MvcResult login(String email, String password) throws Exception {
        return mvc.perform(post("/api/v1/auth/login").with(r -> {
            r.setRemoteAddr(freshOrigin());
            return r;
        }).contentType(MediaType.APPLICATION_JSON)
            .content("{\"email\":\"" + email + "\",\"password\":\"" + password + "\"}")).andReturn();
    }

    /** Waits until {@code count} messages for the address were captured and returns the tokens found in their links. */
    private List<String> awaitTokens(String email, int count) {
        Awaitility.await().atMost(Duration.ofSeconds(20)).pollInterval(Duration.ofMillis(200))
            .until(() -> resetMails(email).size() >= count);
        return tokensOf(email);
    }

    private static List<MailpitSupport.Captured> resetMails(String email) throws Exception {
        return MailpitSupport.messagesTo(email).stream().filter(m -> m.subject().startsWith("Redefinição")).toList();
    }

    private static List<String> tokensOf(String email) {
        try {
            List<String> tokens = new ArrayList<>();
            for (var mail : resetMails(email)) {
                Matcher matcher = LINK.matcher(mail.text());
                assertTrue(matcher.find(), mail.text());
                tokens.add(matcher.group(1));
            }
            return tokens;
        } catch (Exception failure) {
            throw new IllegalStateException(failure);
        }
    }

    private static String code(MvcResult r) throws Exception {
        return JsonPath.read(r.getResponse().getContentAsString(), "$.code");
    }

    @Test
    void existingAndUnknownAccountsGetTheSameAnswerButOnlyTheFirstGetsAnEmail() throws Exception {
        String known = newEmail();
        String unknown = newEmail();
        register(known);

        MvcResult a = requestReset(known);
        MvcResult b = requestReset(unknown);

        assertEquals(202, a.getResponse().getStatus());
        assertEquals(202, b.getResponse().getStatus());
        assertEquals(a.getResponse().getContentAsString(), b.getResponse().getContentAsString());
        assertEquals("Se o e-mail estiver cadastrado, enviaremos um link para redefinir a senha.",
            JsonPath.read(a.getResponse().getContentAsString(), "$.message"));
        awaitTokens(known, 1);
        Thread.sleep(1500);
        assertEquals(0, MailpitSupport.messagesTo(unknown).size());
    }

    @Test
    void invalidEmailFormatIs400() throws Exception {
        assertEquals(400, requestReset("not-an-email").getResponse().getStatus());
        assertEquals(400, requestReset("").getResponse().getStatus());
    }

    @Test
    void theEmailCarriesTheLinkAndValidityNoPasswordAndOnlyTheHashIsStored() throws Exception {
        String email = newEmail();
        register(email);

        requestReset(email);
        String token = awaitTokens(email, 1).get(0);

        var mail = resetMails(email).get(0);
        assertEquals("Redefinição de senha - Finance Controller", mail.subject());
        assertEquals("no-reply@finance-controller.local", mail.from());
        assertTrue(mail.text().contains("30 minutos"));
        assertTrue(mail.text().contains("Se não foi você, ignore esta mensagem"));
        assertTrue(mail.html().contains("redefinir-senha?token=" + token));
        assertFalse(mail.text().contains(PASSWORD));
        assertEquals(43, token.length(), "256 bits em base64url sem padding");

        List<String> hashes = jdbc.queryForList("SELECT t.token_hash FROM password_reset_token t "
            + "JOIN app_user u ON u.id = t.user_id WHERE u.email = ?", String.class, email);
        assertEquals(List.of(ResetToken.hash(token)), hashes);
        assertNotEquals(token, hashes.get(0));
        assertEquals(64, hashes.get(0).length());
    }

    @Test
    void aValidTokenSetsTheNewPasswordOnceAndRevokesEverySessionAndNotifiesByEmail() throws Exception {
        String email = newEmail();
        MvcResult registered = register(email);
        Cookie oldSession = registered.getResponse().getCookie("refresh_token");
        requestReset(email);
        String token = awaitTokens(email, 1).get(0);

        MvcResult done = confirm(token, NEW_PASSWORD);

        assertEquals(204, done.getResponse().getStatus(), done.getResponse().getContentAsString());
        assertEquals(401, login(email, PASSWORD).getResponse().getStatus(), "a senha antiga deixou de valer");
        assertEquals(200, login(email, NEW_PASSWORD).getResponse().getStatus());
        assertEquals(401, mvc.perform(post("/api/v1/auth/refresh").cookie(oldSession)).andReturn().getResponse().getStatus(),
            "a sessão anterior foi revogada");
        assertTrue(done.getResponse().getCookies().length == 0, "não autentica automaticamente");

        MvcResult reuse = confirm(token, NEW_PASSWORD + "x");
        assertEquals(400, reuse.getResponse().getStatus());
        assertEquals("PASSWORD_RESET_TOKEN_INVALID", code(reuse));

        Awaitility.await().atMost(Duration.ofSeconds(20)).until(() -> MailpitSupport.messagesTo(email).stream()
            .anyMatch(m -> m.subject().startsWith("Sua senha foi alterada")));
    }

    @Test
    void unknownAlteredAndExpiredTokensAllGetTheSameError() throws Exception {
        String email = newEmail();
        register(email);
        requestReset(email);
        String token = awaitTokens(email, 1).get(0);
        String altered = token.substring(0, token.length() - 1) + (token.endsWith("A") ? "B" : "A");

        MvcResult unknown = confirm("a".repeat(43), NEW_PASSWORD);
        MvcResult tampered = confirm(altered, NEW_PASSWORD);
        jdbc.update("UPDATE password_reset_token SET expires_at = now() - interval '1 minute' WHERE token_hash = ?",
            ResetToken.hash(token));
        MvcResult expired = confirm(token, NEW_PASSWORD);

        for (MvcResult r : List.of(unknown, tampered, expired)) {
            assertEquals(400, r.getResponse().getStatus());
            assertEquals("PASSWORD_RESET_TOKEN_INVALID", code(r));
        }
        for (String field : List.of("$.title", "$.detail", "$.status")) {
            assertEquals(JsonPath.read(unknown.getResponse().getContentAsString(), field).toString(),
                JsonPath.read(expired.getResponse().getContentAsString(), field).toString(), field);
        }
        assertEquals(200, login(email, PASSWORD).getResponse().getStatus(), "nada mudou");
    }

    @Test
    void aNewRequestInvalidatesThePreviousTokens() throws Exception {
        String email = newEmail();
        register(email);
        requestReset(email);
        String first = awaitTokens(email, 1).get(0);
        requestReset(email);
        List<String> both = awaitTokens(email, 2);
        String second = both.stream().filter(t -> !t.equals(first)).findFirst().orElseThrow();

        MvcResult old = confirm(first, NEW_PASSWORD);
        assertEquals(400, old.getResponse().getStatus());
        assertEquals("PASSWORD_RESET_TOKEN_INVALID", code(old));
        assertEquals(204, confirm(second, NEW_PASSWORD).getResponse().getStatus());
    }

    @Test
    void aWeakPasswordIsRefusedWithoutConsumingTheToken() throws Exception {
        String email = newEmail();
        register(email);
        requestReset(email);
        String token = awaitTokens(email, 1).get(0);

        for (String weak : List.of("short", "12345678", "a".repeat(129))) {
            MvcResult r = confirm(token, weak);
            assertEquals(400, r.getResponse().getStatus(), weak.length() + "");
            assertEquals("VALIDATION_FAILED", code(r));
        }
        assertEquals(204, confirm(token, NEW_PASSWORD).getResponse().getStatus(), "o token seguia valendo");
    }

    @Test
    void aBlankTokenIs400() throws Exception {
        MvcResult r = confirm("", NEW_PASSWORD);
        assertEquals(400, r.getResponse().getStatus());
    }

    @Test
    void morePasswordResetRequestsThanTheLimitStayNeutralButSendNoMoreEmail() throws Exception {
        String email = newEmail();
        register(email);

        for (int i = 0; i < 7; i++) {
            assertEquals(202, requestReset(email).getResponse().getStatus());
        }
        awaitTokens(email, 5);
        Thread.sleep(2000);

        assertEquals(5, resetMails(email).size(), "o limite é de 5 por hora por e-mail");
    }

    @Test
    void twoSimultaneousConfirmsWithTheSameTokenHaveASingleWinner() throws Exception {
        String email = newEmail();
        register(email);
        requestReset(email);
        String token = awaitTokens(email, 1).get(0);

        int threads = 8;
        ExecutorService pool = Executors.newFixedThreadPool(threads);
        CountDownLatch go = new CountDownLatch(1);
        List<Future<Integer>> results = new ArrayList<>();
        for (int i = 0; i < threads; i++) {
            String password = "Senha-Simultanea-" + i + "-2026";
            results.add(pool.submit(() -> {
                go.await();
                return confirm(token, password).getResponse().getStatus();
            }));
        }
        go.countDown();
        int winners = 0;
        int refused = 0;
        for (Future<Integer> result : results) {
            int status = result.get();
            if (status == 204) {
                winners++;
            } else if (status == 400) {
                refused++;
            }
        }
        pool.shutdown();

        assertEquals(1, winners);
        assertEquals(threads - 1, refused);
    }

    @Test
    void expiredTokensAreCleanedUpButValidOnesStay() throws Exception {
        String email = newEmail();
        register(email);
        requestReset(email);
        String token = awaitTokens(email, 1).get(0);
        jdbc.update("INSERT INTO password_reset_token (id, user_id, token_hash, expires_at, created_at) "
            + "SELECT ?, id, ?, now() - interval '3 days', now() - interval '3 days' FROM app_user WHERE email = ?",
            UUID.randomUUID(), "f".repeat(64), email);

        int removed = tokenRepository.deleteExpiredBefore(Instant.now().minus(Duration.ofDays(1)));

        assertTrue(removed >= 1);
        assertEquals(1, jdbc.queryForObject("SELECT count(*) FROM password_reset_token t JOIN app_user u ON u.id = t.user_id "
            + "WHERE u.email = ?", Integer.class, email));
        assertEquals(204, confirm(token, NEW_PASSWORD).getResponse().getStatus());
    }
}
