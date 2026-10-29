package com.dowglasmaia.financecontroller.operations;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

import com.dowglasmaia.financecontroller.support.AbstractPostgresIntegrationTest;
import com.jayway.jsonpath.JsonPath;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
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

/**
 * QUALITY-SPEC-05, desempenho: 50 000 lançamentos em 5 anos e 2 moedas para o utilizador medido e outros 50 000 para um
 * segundo utilizador (prova que o volume do outro não pesa nem se mistura). Mede o p95 de {@code listTransactions},
 * {@code getMonthlyOverview} e {@code getReport(YEAR)} e escreve as medições no log como {@code PERF ...}. As metas
 * aprovadas (p95 ≤ 300 / 500 / 800 ms) são assertivas.
 */
@AutoConfigureMockMvc
class PerformanceIntegrationTest extends AbstractPostgresIntegrationTest {
    private static final String PASSWORD = "Sup3r-Secret-Passw0rd";
    private static final int ROWS_PER_USER = 50_000;
    private static final int WARM_UP = 10;
    private static final int SAMPLES = 40;

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
        registry.add("app.identity.jwt.secret", () -> "test-only-dummy-jwt-secret-0123456789-abcdef");
    }

    @Autowired
    private MockMvc mvc;

    @Autowired
    private JdbcTemplate jdbc;

    private record User(String id, String token) { }

    private User newUser() throws Exception {
        String email = "perf." + UUID.randomUUID() + "@example.com";
        MvcResult result = mvc.perform(post("/api/v1/auth/register").with(r -> {
            r.setRemoteAddr("10.88." + (int) (Math.random() * 250) + "." + (int) (Math.random() * 250));
            return r;
        }).contentType(MediaType.APPLICATION_JSON)
            .content("{\"email\":\"" + email + "\",\"password\":\"" + PASSWORD + "\"}")).andReturn();
        assertEquals(201, result.getResponse().getStatus());
        String body = result.getResponse().getContentAsString();
        return new User(JsonPath.read(body, "$.user.id"), JsonPath.read(body, "$.accessToken"));
    }

    /** 50 000 lançamentos determinísticos: 5 anos, BRL e EUR, 1/3 entradas, categorias do próprio utilizador. */
    private void seed(User user) {
        jdbc.update("""
            INSERT INTO transaction_entry (id, owner_id, kind, description, amount, currency, entry_date, category_id, created_at, updated_at)
            SELECT gen_random_uuid(), ?::uuid,
                   CASE WHEN i % 3 = 0 THEN 'INCOME' ELSE 'EXPENSE' END,
                   'Lancamento sintetico ' || i,
                   ((i * 37) % 50000) / 100.0 + 1,
                   CASE WHEN i % 4 = 0 THEN 'EUR' ELSE 'BRL' END,
                   DATE '2022-01-01' + (i % 1826),
                   CASE WHEN i % 3 = 0 THEN (SELECT (array_agg(id))[1 + i % 4] FROM category WHERE owner_id = ?::uuid AND type = 'INCOME')
                        ELSE (SELECT (array_agg(id))[1 + i % 7] FROM category WHERE owner_id = ?::uuid AND type = 'EXPENSE') END,
                   now(), now()
            FROM generate_series(1, ?) AS i
            """, user.id(), user.id(), user.id(), ROWS_PER_USER);
        jdbc.execute("ANALYZE transaction_entry");
    }

    private long millis(String path, User user) throws Exception {
        long start = System.nanoTime();
        MvcResult result = mvc.perform(get(path).header(HttpHeaders.AUTHORIZATION, "Bearer " + user.token())).andReturn();
        long elapsed = (System.nanoTime() - start) / 1_000_000;
        assertEquals(200, result.getResponse().getStatus(), path);
        return elapsed;
    }

    private long p95(String label, String path, User user, long limitMs) throws Exception {
        for (int i = 0; i < WARM_UP; i++) {
            millis(path, user);
        }
        List<Long> samples = new ArrayList<>();
        for (int i = 0; i < SAMPLES; i++) {
            samples.add(millis(path, user));
        }
        Collections.sort(samples);
        long p95 = samples.get((int) Math.ceil(SAMPLES * 0.95) - 1);
        System.out.println("PERF " + label + " rows=" + (2 * ROWS_PER_USER) + " p50=" + samples.get(SAMPLES / 2) + "ms p95=" + p95
            + "ms max=" + samples.get(SAMPLES - 1) + "ms limit=" + limitMs + "ms");
        assertTrue(p95 <= limitMs, label + " p95 " + p95 + " ms acima da meta de " + limitMs + " ms");
        return p95;
    }

    @Test
    void theMainReadsStayWithinTheirTargetsWithFiftyThousandRowsPerUser() throws Exception {
        User measured = newUser();
        User neighbour = newUser();
        seed(measured);
        seed(neighbour);

        p95("listTransactions", "/api/v1/transactions?month=2026-06&currency=BRL&page=1&size=15", measured, 300);
        p95("getMonthlyOverview", "/api/v1/overview?month=2026-06&currency=BRL", measured, 500);
        p95("getReport(YEAR)", "/api/v1/reports?period=YEAR&anchorDate=2026-06-15&currency=BRL", measured, 800);
        p95("getReport(MONTH)", "/api/v1/reports?period=MONTH&anchorDate=2026-06-15&currency=BRL", measured, 500);
    }

    @Test
    void theTotalsOfTheMeasuredUserNeverIncludeTheNeighboursVolume() throws Exception {
        User measured = newUser();
        User neighbour = newUser();
        seed(measured);
        seed(neighbour);

        MvcResult overview = mvc.perform(get("/api/v1/overview?month=2026-06&currency=BRL")
            .header(HttpHeaders.AUTHORIZATION, "Bearer " + measured.token())).andReturn();

        BigDecimal expectedExpense = jdbc.queryForObject("SELECT coalesce(sum(amount), 0) FROM transaction_entry WHERE owner_id = ?::uuid "
            + "AND currency = 'BRL' AND kind = 'EXPENSE' AND entry_date BETWEEN DATE '2026-06-01' AND DATE '2026-06-30'",
            BigDecimal.class, measured.id());
        BigDecimal both = jdbc.queryForObject("SELECT coalesce(sum(amount), 0) FROM transaction_entry WHERE "
            + "currency = 'BRL' AND kind = 'EXPENSE' AND entry_date BETWEEN DATE '2026-06-01' AND DATE '2026-06-30' "
            + "AND owner_id IN (?::uuid, ?::uuid)", BigDecimal.class, measured.id(), neighbour.id());
        BigDecimal reported = new BigDecimal((String) JsonPath.read(overview.getResponse().getContentAsString(), "$.summary.expense"));

        assertEquals(0, expectedExpense.compareTo(reported), "o total é só do utilizador medido");
        assertTrue(both.compareTo(reported) > 0, "o vizinho tem dados no mesmo mês e moeda");
    }
}
