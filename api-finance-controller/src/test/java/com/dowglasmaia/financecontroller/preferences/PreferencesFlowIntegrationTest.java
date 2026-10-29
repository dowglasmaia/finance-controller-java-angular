package com.dowglasmaia.financecontroller.preferences;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;

import com.dowglasmaia.financecontroller.support.AbstractPostgresIntegrationTest;
import com.jayway.jsonpath.JsonPath;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

/** Preferences through the real HTTP stack and a real PostgreSQL: defaults, persistence, isolation, validation, races. */
@AutoConfigureMockMvc
class PreferencesFlowIntegrationTest extends AbstractPostgresIntegrationTest {
    private static final String PASSWORD = "Sup3r-Secret-Passw0rd";

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
        registry.add("app.identity.jwt.secret", () -> "test-only-dummy-jwt-secret-0123456789-abcdef");
    }

    @Autowired
    private MockMvc mvc;

    @Autowired
    private JdbcTemplate jdbc;

    private String lastEmail;

    private int rowsOfLastUser() {
        return jdbc.queryForObject("SELECT count(*) FROM preference p JOIN app_user u ON u.id = p.owner_id WHERE u.email = ?",
            Integer.class, lastEmail);
    }

    private String newUserToken() throws Exception {
        String email = "pref." + UUID.randomUUID() + "@example.com";
        lastEmail = email;
        MvcResult result = mvc.perform(post("/api/v1/auth/register").with(r -> {
            r.setRemoteAddr("10.11." + (int) (Math.random() * 250) + "." + (int) (Math.random() * 250));
            return r;
        }).contentType(MediaType.APPLICATION_JSON)
            .content("{\"email\":\"" + email + "\",\"password\":\"" + PASSWORD + "\"}")).andReturn();
        assertEquals(201, result.getResponse().getStatus());
        return JsonPath.read(result.getResponse().getContentAsString(), "$.accessToken");
    }

    private MvcResult call(MockHttpServletRequestBuilder request, String token) throws Exception {
        return mvc.perform(request.header(HttpHeaders.AUTHORIZATION, "Bearer " + token)).andReturn();
    }

    private MvcResult save(String token, String body) throws Exception {
        return call(put("/api/v1/preferences").contentType(MediaType.APPLICATION_JSON).content(body), token);
    }

    private static String field(MvcResult r, String path) throws Exception {
        return JsonPath.read(r.getResponse().getContentAsString(), path);
    }

    @Test
    void requiresAuthentication() throws Exception {
        assertEquals(401, mvc.perform(get("/api/v1/preferences")).andReturn().getResponse().getStatus());
        assertEquals(401, mvc.perform(put("/api/v1/preferences").contentType(MediaType.APPLICATION_JSON)
            .content("{\"currency\":\"BRL\",\"theme\":\"DARK\"}")).andReturn().getResponse().getStatus());
    }

    @Test
    void aNewUserGetsBrlAndDarkWithoutAnythingBeingWritten() throws Exception {
        String token = newUserToken();

        MvcResult result = call(get("/api/v1/preferences"), token);

        assertEquals(200, result.getResponse().getStatus());
        assertEquals("BRL", field(result, "$.currency"));
        assertEquals("DARK", field(result, "$.theme"));
        assertEquals(0, rowsOfLastUser(), "ler não grava nada");
    }

    @Test
    void savedValuesComeBackTheSameOnAnotherRead() throws Exception {
        String token = newUserToken();

        MvcResult saved = save(token, "{\"currency\":\"EUR\",\"theme\":\"LIGHT\"}");
        assertEquals(200, saved.getResponse().getStatus(), saved.getResponse().getContentAsString());
        assertEquals("EUR", field(saved, "$.currency"));
        assertEquals("LIGHT", field(saved, "$.theme"));

        MvcResult read = call(get("/api/v1/preferences"), token);
        assertEquals("EUR", field(read, "$.currency"));
        assertEquals("LIGHT", field(read, "$.theme"));

        MvcResult again = save(token, "{\"currency\":\"BRL\",\"theme\":\"SYSTEM\"}");
        assertEquals("SYSTEM", field(again, "$.theme"));
        assertEquals("BRL", field(call(get("/api/v1/preferences"), token), "$.currency"));
        assertEquals(1, rowsOfLastUser(), "duas gravações, uma única linha");
    }

    @Test
    void oneUserNeverReadsOrChangesAnothers() throws Exception {
        String a = newUserToken();
        String b = newUserToken();

        save(a, "{\"currency\":\"EUR\",\"theme\":\"LIGHT\"}");

        MvcResult bRead = call(get("/api/v1/preferences"), b);
        assertEquals("BRL", field(bRead, "$.currency"));
        assertEquals("DARK", field(bRead, "$.theme"));
        save(b, "{\"currency\":\"BRL\",\"theme\":\"SYSTEM\"}");
        assertEquals("EUR", field(call(get("/api/v1/preferences"), a), "$.currency"));
        assertEquals("LIGHT", field(call(get("/api/v1/preferences"), a), "$.theme"));
    }

    @Test
    void unknownOrMissingValuesAreRejected() throws Exception {
        String token = newUserToken();
        save(token, "{\"currency\":\"EUR\",\"theme\":\"LIGHT\"}");

        for (String body : List.of(
            "{\"currency\":\"USD\",\"theme\":\"DARK\"}",
            "{\"currency\":\"BRL\",\"theme\":\"NEON\"}",
            "{\"currency\":\"BRL\"}",
            "{\"theme\":\"DARK\"}",
            "{}",
            "not json")) {
            MvcResult result = save(token, body);
            assertEquals(400, result.getResponse().getStatus(), body);
            assertEquals("VALIDATION_FAILED", field(result, "$.code"), body);
        }
        // nada mudou
        assertEquals("EUR", field(call(get("/api/v1/preferences"), token), "$.currency"));
    }

    @Test
    void simultaneousSavesNeverCollideOnTheUniqueKey() throws Exception {
        String token = newUserToken();
        int threads = 8;
        ExecutorService pool = Executors.newFixedThreadPool(threads);
        CountDownLatch go = new CountDownLatch(1);
        List<Future<Integer>> results = new ArrayList<>();
        for (int i = 0; i < threads; i++) {
            String body = "{\"currency\":\"" + (i % 2 == 0 ? "BRL" : "EUR") + "\",\"theme\":\"DARK\"}";
            results.add(pool.submit(() -> {
                go.await();
                return save(token, body).getResponse().getStatus();
            }));
        }
        go.countDown();
        for (Future<Integer> result : results) {
            assertEquals(200, result.get());
        }
        pool.shutdown();

        String currency = field(call(get("/api/v1/preferences"), token), "$.currency");
        assertEquals(true, currency.equals("BRL") || currency.equals("EUR"));
    }
}
