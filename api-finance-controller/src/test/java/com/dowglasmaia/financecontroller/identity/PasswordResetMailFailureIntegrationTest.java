package com.dowglasmaia.financecontroller.identity;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

import com.dowglasmaia.financecontroller.identity.domain.ResetToken;
import com.dowglasmaia.financecontroller.support.AbstractPostgresIntegrationTest;
import java.time.Duration;
import java.util.UUID;
import org.awaitility.Awaitility;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

/**
 * With the mail server unreachable the request still answers the same neutral 202, the failure shows up only as an
 * internal reference in the log, and neither the e-mail address nor the token ever reaches the logs.
 */
@AutoConfigureMockMvc
@ExtendWith(OutputCaptureExtension.class)
class PasswordResetMailFailureIntegrationTest extends AbstractPostgresIntegrationTest {

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
        registry.add("app.identity.jwt.secret", () -> "test-only-dummy-jwt-secret-0123456789-abcdef");
        registry.add("spring.mail.host", () -> "127.0.0.1");
        registry.add("spring.mail.port", () -> "1"); // nothing listens here
        registry.add("spring.mail.properties.mail.smtp.connectiontimeout", () -> "1000");
    }

    @Autowired
    private MockMvc mvc;

    @Autowired
    private JdbcTemplate jdbc;

    @Test
    void aMailServerFailureNeitherChangesTheAnswerNorLeaksTheAddressOrTheToken(CapturedOutput output) throws Exception {
        String email = "mailfail." + UUID.randomUUID() + "@example.com";
        mvc.perform(post("/api/v1/auth/register").with(r -> {
            r.setRemoteAddr("10.77.1.1");
            return r;
        }).contentType(MediaType.APPLICATION_JSON)
            .content("{\"email\":\"" + email + "\",\"password\":\"Sup3r-Secret-Passw0rd\"}")).andReturn();

        MvcResult known = request(email, "10.77.2.2");
        MvcResult unknown = request("nobody." + UUID.randomUUID() + "@example.com", "10.77.3.3");

        assertEquals(202, known.getResponse().getStatus());
        assertEquals(known.getResponse().getContentAsString(), unknown.getResponse().getContentAsString());
        Awaitility.await().atMost(Duration.ofSeconds(20)).until(() -> output.getAll().contains("mail not sent ref="));
        assertEquals(1, jdbc.queryForObject("SELECT count(*) FROM password_reset_token t JOIN app_user u ON u.id = t.user_id "
            + "WHERE u.email = ?", Integer.class, email), "o token foi criado mesmo sem o e-mail sair");
        assertFalse(output.getAll().contains(email), "o e-mail da conta nunca entra no log");
        assertTrue(output.getAll().contains("cause="), "a causa é registada só pelo tipo");
        for (String hash : jdbc.queryForList("SELECT token_hash FROM password_reset_token", String.class)) {
            assertFalse(output.getAll().contains(hash));
        }
        assertTrue(ResetToken.generate().toString().contains("redacted"));
    }

    private MvcResult request(String email, String origin) throws Exception {
        return mvc.perform(post("/api/v1/auth/password-reset/request").with(r -> {
            r.setRemoteAddr(origin);
            return r;
        }).contentType(MediaType.APPLICATION_JSON).content("{\"email\":\"" + email + "\"}")).andReturn();
    }
}
