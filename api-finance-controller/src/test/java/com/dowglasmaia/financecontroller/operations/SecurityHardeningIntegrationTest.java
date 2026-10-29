package com.dowglasmaia.financecontroller.operations;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;

import com.dowglasmaia.financecontroller.support.AbstractPostgresIntegrationTest;
import com.jayway.jsonpath.JsonPath;
import com.nimbusds.jose.jwk.source.ImmutableSecret;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

/**
 * QUALITY-SPEC-05 on the real stack: hostile and extreme input, security headers, CORS, error bodies without internals,
 * every data route behind a session, and logs without personal or financial values.
 */
@AutoConfigureMockMvc
@ExtendWith(OutputCaptureExtension.class)
class SecurityHardeningIntegrationTest extends AbstractPostgresIntegrationTest {
    private static final String PASSWORD = "Sup3r-Secret-Passw0rd";
    private static final String ALLOWED_ORIGIN = "http://localhost:4200";

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
        registry.add("app.identity.jwt.secret", () -> "test-only-dummy-jwt-secret-0123456789-abcdef");
        registry.add("app.identity.cors.allowed-origins", () -> ALLOWED_ORIGIN);
    }

    @Autowired
    private MockMvc mvc;

    @Autowired
    private JdbcTemplate jdbc;

    @Autowired
    private JwtEncoder currentSecretEncoder;

    private String lastEmail;

    private String newUserToken() throws Exception {
        lastEmail = "hard." + UUID.randomUUID() + "@example.com";
        MvcResult result = mvc.perform(post("/api/v1/auth/register").with(r -> {
            r.setRemoteAddr("10.12." + (int) (Math.random() * 250) + "." + (int) (Math.random() * 250));
            return r;
        }).contentType(MediaType.APPLICATION_JSON)
            .content("{\"email\":\"" + lastEmail + "\",\"password\":\"" + PASSWORD + "\"}")).andReturn();
        assertEquals(201, result.getResponse().getStatus());
        return JsonPath.read(result.getResponse().getContentAsString(), "$.accessToken");
    }

    private MvcResult call(MockHttpServletRequestBuilder request, String token) throws Exception {
        return mvc.perform(request.header(HttpHeaders.AUTHORIZATION, "Bearer " + token)).andReturn();
    }

    private static void assertNoInternals(MvcResult result) throws Exception {
        String body = result.getResponse().getContentAsString();
        for (String leak : new String[] {"Exception", "\tat ", "at com.", "at org.", "org.springframework", "org.hibernate",
            "org.postgresql", "SELECT ", "INSERT ", "stackTrace", "jdbc:", "SQLState", "PSQL"}) {
            assertFalse(body.contains(leak), "vazamento '" + leak + "' em: " + body);
        }
    }

    private static String category(String token, MockMvc mvc) throws Exception {
        MvcResult list = mvc.perform(get("/api/v1/categories").header(HttpHeaders.AUTHORIZATION, "Bearer " + token)).andReturn();
        List<String> ids = JsonPath.read(list.getResponse().getContentAsString(), "$[?(@.name=='Alimentação')].id");
        return ids.get(0);
    }

    // ------------------------------------------------------------------ entrada

    @Test
    void malformedAndWrongTypedJsonIs400WithoutInternals() throws Exception {
        String token = newUserToken();
        for (String body : List.of("{not json", "", "[]", "\"x\"", "{\"name\":", "{\"name\":{\"a\":1},\"type\":\"EXPENSE\"}",
            "{\"email\":12345,\"password\":true}", "\u0000\u0001\u0002")) {
            MvcResult result = call(post("/api/v1/categories").contentType(MediaType.APPLICATION_JSON).content(body), token);
            assertEquals(400, result.getResponse().getStatus(), body);
            assertNoInternals(result);
        }
        MvcResult login = mvc.perform(post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON).content("{not json")).andReturn();
        assertEquals(400, login.getResponse().getStatus());
        assertNoInternals(login);
    }

    @Test
    void aBodyAboveTheLimitIsRefusedBeforeItIsRead() throws Exception {
        String token = newUserToken();
        String huge = "{\"name\":\"" + "a".repeat(2 * 1024 * 1024) + "\",\"type\":\"EXPENSE\",\"icon\":\"x\",\"color\":\"#FF9800\"}";

        MvcResult result = call(post("/api/v1/categories").contentType(MediaType.APPLICATION_JSON).content(huge), token);

        assertEquals(413, result.getResponse().getStatus());
        assertNoInternals(result);
    }

    @Test
    void veryLongStringsAndExtremeNumbersAreRefusedNotStoredNorCrashing() throws Exception {
        String token = newUserToken();
        String food = category(token, mvc);
        String longName = "n".repeat(10_000);
        assertEquals(400, call(post("/api/v1/categories").contentType(MediaType.APPLICATION_JSON)
            .content("{\"name\":\"" + longName + "\",\"type\":\"EXPENSE\",\"icon\":\"x\",\"color\":\"#FF9800\"}"), token)
            .getResponse().getStatus());
        for (String amount : List.of("99999999999.99", "1e400", "-1", "0", "NaN", "Infinity", "1.234", "1,5", "", " ", "0x10", "9".repeat(400))) {
            MvcResult result = call(post("/api/v1/transactions").contentType(MediaType.APPLICATION_JSON)
                .content("{\"kind\":\"EXPENSE\",\"description\":\"x\",\"amount\":\"" + amount + "\",\"currency\":\"BRL\","
                    + "\"date\":\"2026-06-10\",\"categoryId\":\"" + food + "\"}"), token);
            assertEquals(400, result.getResponse().getStatus(), "amount '" + (amount.length() > 20 ? "9x400" : amount) + "'");
            assertNoInternals(result);
        }
        for (String date : List.of("0000-01-01", "9999-12-31", "2026-02-30", "not-a-date", "2026-6-1")) {
            assertEquals(400, call(post("/api/v1/transactions").contentType(MediaType.APPLICATION_JSON)
                .content("{\"kind\":\"EXPENSE\",\"description\":\"x\",\"amount\":\"1.00\",\"currency\":\"BRL\",\"date\":\"" + date
                    + "\",\"categoryId\":\"" + food + "\"}"), token).getResponse().getStatus(), date);
        }
    }

    @Test
    void controlCharactersNeverCauseAServerError() throws Exception {
        String token = newUserToken();
        for (String name : List.of("a\\u0000b", "tab\\there", "line\\nbreak", "bell\\u0007", "\\u202Ertl", "emoji \\ud83d\\ude00")) {
            MvcResult result = call(post("/api/v1/categories").contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"" + name + "\",\"type\":\"EXPENSE\",\"icon\":\"x\",\"color\":\"#FF9800\"}"), token);
            assertTrue(result.getResponse().getStatus() < 500, name + " -> " + result.getResponse().getStatus() + " "
                + result.getResponse().getContentAsString());
            assertNoInternals(result);
        }
    }

    @Test
    void sqlAndMarkupInTextAreStoredAsPlainTextAndTheSchemaSurvives() throws Exception {
        String token = newUserToken();
        String payload = "'; DROP TABLE app_user; --";
        MvcResult created = call(post("/api/v1/categories").contentType(MediaType.APPLICATION_JSON)
            .content("{\"name\":\"" + payload + "\",\"type\":\"EXPENSE\",\"icon\":\"x\",\"color\":\"#FF9800\"}"), token);
        assertEquals(201, created.getResponse().getStatus(), created.getResponse().getContentAsString());
        assertEquals(payload, JsonPath.read(created.getResponse().getContentAsString(), "$.name"));
        MvcResult script = call(post("/api/v1/categories").contentType(MediaType.APPLICATION_JSON)
            .content("{\"name\":\"<script>alert(1)</script>\",\"type\":\"EXPENSE\",\"icon\":\"x\",\"color\":\"#FF9800\"}"), token);
        assertEquals(201, script.getResponse().getStatus());
        assertTrue(jdbc.queryForObject("SELECT count(*) FROM app_user", Integer.class) > 0, "a tabela continua existindo");
        for (String query : List.of("/api/v1/transactions?month=2026-06&currency=BRL'%20OR%201=1--",
            "/api/v1/transactions?month=2026-06%27%3B%20DROP%20TABLE%20app_user&currency=BRL")) {
            MvcResult result = call(get(query), token);
            assertEquals(400, result.getResponse().getStatus(), query);
            assertNoInternals(result);
        }
    }

    // ------------------------------------------------------------------ erros sem detalhes

    @Test
    void routingAndTypeErrorsHaveProblemBodiesWithoutInternals() throws Exception {
        String token = newUserToken();
        record Case(MockHttpServletRequestBuilder request, int status) { }
        List<Case> cases = List.of(
            new Case(get("/api/v1/nao-existe"), 404),
            new Case(get("/api/v1/categories/not-a-uuid"), 400),
            new Case(get("/api/v1/money-boxes?currency=XYZ"), 400),
            new Case(get("/api/v1/transactions?month=2026-06"), 400),
            new Case(delete("/api/v1/categories"), 405),
            new Case(put("/api/v1/auth/login"), 405),
            new Case(post("/api/v1/categories").contentType(MediaType.TEXT_PLAIN).content("x"), 415),
            new Case(get("/api/v1/categories").accept(MediaType.APPLICATION_XML), 406));
        for (Case c : cases) {
            MvcResult result = call(c.request(), token);
            assertEquals(c.status(), result.getResponse().getStatus(), c.request().toString());
            assertNoInternals(result);
            String body = result.getResponse().getContentAsString();
            if (!body.isBlank()) {
                assertNotNull(JsonPath.read(body, "$.code"));
            }
        }
    }

    // ------------------------------------------------------------------ cabeçalhos e CORS

    @Test
    void securityHeadersAreOnEveryResponseAndHstsOnHttps() throws Exception {
        String token = newUserToken();

        MvcResult api = call(get("/api/v1/categories").secure(true), token);
        MvcResult unauthenticated = mvc.perform(get("/api/v1/categories")).andReturn();
        MvcResult notFound = call(get("/api/v1/nao-existe"), token);

        for (MvcResult result : List.of(api, unauthenticated, notFound)) {
            var response = result.getResponse();
            assertEquals("nosniff", response.getHeader("X-Content-Type-Options"));
            assertEquals("DENY", response.getHeader("X-Frame-Options"));
            assertTrue(response.getHeader("Content-Security-Policy").contains("default-src 'none'"));
            assertEquals("no-referrer", response.getHeader("Referrer-Policy"));
            assertTrue(response.getHeader("Cache-Control").contains("no-store"));
            assertNotNull(response.getHeader("X-Request-Id"));
            assertFalse(response.containsHeader("X-Powered-By"));
            assertFalse(response.containsHeader("Server"));
        }
        assertTrue(api.getResponse().getHeader("Strict-Transport-Security").contains("max-age="));
    }

    @Test
    void anUnauthorizedOriginIsRefusedAndTheAllowedOneIsAnswered() throws Exception {
        String token = newUserToken();

        MvcResult evil = call(get("/api/v1/categories").header("Origin", "https://evil.example"), token);
        MvcResult evilPreflight = mvc.perform(options("/api/v1/auth/login").header("Origin", "https://evil.example")
            .header("Access-Control-Request-Method", "POST")).andReturn();
        MvcResult okPreflight = mvc.perform(options("/api/v1/auth/login").header("Origin", ALLOWED_ORIGIN)
            .header("Access-Control-Request-Method", "POST")).andReturn();
        MvcResult okCall = call(get("/api/v1/categories").header("Origin", ALLOWED_ORIGIN), token);

        assertEquals(403, evil.getResponse().getStatus());
        assertEquals(403, evilPreflight.getResponse().getStatus());
        assertFalse(evil.getResponse().containsHeader("Access-Control-Allow-Origin"));
        assertEquals(200, okPreflight.getResponse().getStatus());
        assertEquals(ALLOWED_ORIGIN, okPreflight.getResponse().getHeader("Access-Control-Allow-Origin"));
        assertEquals("true", okPreflight.getResponse().getHeader("Access-Control-Allow-Credentials"));
        assertEquals(200, okCall.getResponse().getStatus());
        // credenciais (cookie de renovação) só nas rotas de autenticação
        MvcResult dataPreflight = mvc.perform(options("/api/v1/categories").header("Origin", ALLOWED_ORIGIN)
            .header("Access-Control-Request-Method", "GET")).andReturn();
        assertFalse("true".equals(dataPreflight.getResponse().getHeader("Access-Control-Allow-Credentials")));
    }

    // ------------------------------------------------------------------ acesso

    @Test
    void everyDataRouteNeedsASessionAndAForgedTokenIsRefused() throws Exception {
        newUserToken();
        UUID id = UUID.randomUUID();
        List<String> routes = List.of("/api/v1/categories", "/api/v1/categories/" + id, "/api/v1/transactions?month=2026-06&currency=BRL",
            "/api/v1/transactions/" + id, "/api/v1/budgets/BRL?month=2026-06", "/api/v1/budgets/BRL/progress?month=2026-06",
            "/api/v1/overview?month=2026-06&currency=BRL", "/api/v1/money-boxes?currency=BRL", "/api/v1/money-boxes/" + id,
            "/api/v1/money-boxes/" + id + "/entries", "/api/v1/reserves/BRL/summary",
            "/api/v1/reports?period=MONTH&anchorDate=2026-06-15&currency=BRL", "/api/v1/preferences", "/api/v1/auth/me");
        for (String route : routes) {
            assertEquals(401, mvc.perform(get(route)).andReturn().getResponse().getStatus(), route);
            assertEquals(401, mvc.perform(get(route).header(HttpHeaders.AUTHORIZATION, "Bearer forged.token.value")).andReturn()
                .getResponse().getStatus(), route + " com token forjado");
        }
        for (String route : List.of("/actuator/env", "/actuator/beans", "/actuator/heapdump", "/actuator/metrics")) {
            int status = mvc.perform(get(route)).andReturn().getResponse().getStatus();
            assertTrue(status == 401 || status == 404, route + " -> " + status);
        }
    }

    // ------------------------------------------------------------------ logs

    @Test
    void logsNeverHoldPasswordsTokensEmailsOrFinancialValues(CapturedOutput output) throws Exception {
        String password = "L0g-Hygiene-Passw0rd-" + UUID.randomUUID();
        String email = "loghygiene." + UUID.randomUUID() + "@example.com";
        MvcResult registered = mvc.perform(post("/api/v1/auth/register").with(r -> {
            r.setRemoteAddr("10.55.1.1");
            return r;
        }).contentType(MediaType.APPLICATION_JSON).content("{\"email\":\"" + email + "\",\"password\":\"" + password + "\"}")).andReturn();
        String access = JsonPath.read(registered.getResponse().getContentAsString(), "$.accessToken");
        String refresh = registered.getResponse().getCookie("refresh_token").getValue();
        String secretDescription = "Descricao-Sigilosa-" + UUID.randomUUID();
        String food = category(access, mvc);

        call(post("/api/v1/transactions").contentType(MediaType.APPLICATION_JSON)
            .content("{\"kind\":\"EXPENSE\",\"description\":\"" + secretDescription + "\",\"amount\":\"7654321.99\",\"currency\":\"BRL\","
                + "\"date\":\"2026-06-10\",\"categoryId\":\"" + food + "\"}"), access);
        call(put("/api/v1/budgets/BRL").contentType(MediaType.APPLICATION_JSON)
            .content("{\"month\":\"2026-06\",\"monthlyLimit\":\"8765432.10\",\"categoryLimits\":[]}"), access);
        call(post("/api/v1/money-boxes").contentType(MediaType.APPLICATION_JSON)
            .content("{\"name\":\"Cofre-Sigiloso\",\"icon\":\"x\",\"color\":\"#22C55E\",\"currency\":\"BRL\",\"goal\":\"9876543.21\"}"), access);
        call(get("/api/v1/overview?month=2026-06&currency=BRL"), access);
        mvc.perform(post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON)
            .content("{\"email\":\"" + email + "\",\"password\":\"senha-errada-123\"}")).andReturn();
        mvc.perform(post("/api/v1/auth/password-reset/request").contentType(MediaType.APPLICATION_JSON)
            .content("{\"email\":\"" + email + "\"}")).andReturn();
        mvc.perform(post("/api/v1/auth/password-reset/confirm").contentType(MediaType.APPLICATION_JSON)
            .content("{\"token\":\"token-de-recuperacao-que-nao-existe\",\"newPassword\":\"" + password + "x\"}")).andReturn();
        Thread.sleep(1500);

        String log = output.getAll();
        for (String secret : new String[] {password, "senha-errada-123", access, refresh, email, secretDescription, "7654321.99",
            "8765432.10", "9876543.21", "Cofre-Sigiloso", "token-de-recuperacao-que-nao-existe"}) {
            assertFalse(log.contains(secret), "o log contém um valor sensível");
        }
        assertTrue(log.contains("request completed"), "as linhas de acesso existem");
    }

    // ------------------------------------------------------------------ rotação do segredo de assinatura

    private static String signed(JwtEncoder encoder, String subject) {
        var now = Instant.now();
        var claims = JwtClaimsSet.builder().issuer("https://api.finance-controller.local").subject(subject)
            .issuedAt(now).expiresAt(now.plusSeconds(600)).build();
        return encoder.encode(JwtEncoderParameters.from(JwsHeader.with(MacAlgorithm.HS256).build(), claims)).getTokenValue();
    }

    /** Guia de operação: trocar APP_JWT_SECRET invalida os access tokens antigos; os novos (segredo novo) valem. */
    @Test
    void rotatingTheSigningSecretInvalidatesTokensSignedWithTheOldOne() throws Exception {
        String real = newUserToken();
        String userId = JsonPath.read(call(get("/api/v1/auth/me"), real).getResponse().getContentAsString(), "$.id");
        var oldSecret = new SecretKeySpecHolder("old-signing-secret-that-is-at-least-32-bytes!!");
        JwtEncoder oldEncoder = new NimbusJwtEncoder(new ImmutableSecret<>(oldSecret.key()));

        assertEquals(200, call(get("/api/v1/auth/me"), signed(currentSecretEncoder, userId)).getResponse().getStatus(),
            "token com o segredo atual vale");
        assertEquals(401, call(get("/api/v1/auth/me"), signed(oldEncoder, userId)).getResponse().getStatus(),
            "token assinado com outro segredo (o antigo, depois da rotação) é recusado");
    }

    private record SecretKeySpecHolder(String secret) {
        javax.crypto.SecretKey key() {
            return new javax.crypto.spec.SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
        }
    }
}
