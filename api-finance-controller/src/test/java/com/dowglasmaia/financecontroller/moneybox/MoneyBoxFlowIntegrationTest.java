package com.dowglasmaia.financecontroller.moneybox;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;

import com.dowglasmaia.financecontroller.support.AbstractPostgresIntegrationTest;
import com.jayway.jsonpath.JsonPath;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Callable;
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

/**
 * Money boxes and reserves through the real HTTP stack and a real PostgreSQL (Testcontainers): balances, the
 * never-negative rule (create, edit, delete), simultaneous withdrawals, locked currency, atomic cascade, goals,
 * isolation between users and currencies, and the reserves total on the dashboard.
 */
@AutoConfigureMockMvc
class MoneyBoxFlowIntegrationTest extends AbstractPostgresIntegrationTest {
    private static final String PASSWORD = "Sup3r-Secret-Passw0rd";

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
        registry.add("app.identity.jwt.secret", () -> "test-only-dummy-jwt-secret-0123456789-abcdef");
    }

    @Autowired
    private MockMvc mvc;

    @Autowired
    private JdbcTemplate jdbc;

    private String newUserToken() throws Exception {
        String email = "box." + UUID.randomUUID() + "@example.com";
        MvcResult result = mvc.perform(post("/api/v1/auth/register").with(r -> {
            r.setRemoteAddr("10.8." + (int) (Math.random() * 250) + "." + (int) (Math.random() * 250));
            return r;
        }).contentType(MediaType.APPLICATION_JSON)
            .content("{\"email\":\"" + email + "\",\"password\":\"" + PASSWORD + "\"}")).andReturn();
        assertEquals(201, result.getResponse().getStatus());
        return JsonPath.read(text(result), "$.accessToken");
    }

    private MvcResult call(MockHttpServletRequestBuilder request, String token) throws Exception {
        return mvc.perform(request.header(HttpHeaders.AUTHORIZATION, "Bearer " + token)).andReturn();
    }

    private static String text(MvcResult r) throws Exception {
        return r.getResponse().getContentAsString();
    }

    private static String json(MvcResult r, String path) throws Exception {
        return JsonPath.read(text(r), path);
    }

    private String createBox(String token, String name, String currency, String goal) throws Exception {
        String goalPart = goal == null ? "" : ",\"goal\":\"" + goal + "\"";
        MvcResult r = call(post("/api/v1/money-boxes").contentType(MediaType.APPLICATION_JSON)
            .content("{\"name\":\"" + name + "\",\"icon\":\"piggy-bank\",\"color\":\"#22C55E\",\"currency\":\"" + currency + "\""
                + goalPart + "}"), token);
        assertEquals(201, r.getResponse().getStatus(), text(r));
        return json(r, "$.id");
    }

    private MvcResult entry(String token, String boxId, String kind, String amount, String date) throws Exception {
        return call(post("/api/v1/money-boxes/" + boxId + "/entries").contentType(MediaType.APPLICATION_JSON)
            .content("{\"kind\":\"" + kind + "\",\"amount\":\"" + amount + "\",\"date\":\"" + date + "\"}"), token);
    }

    private String ok(MvcResult r) throws Exception {
        assertTrue(r.getResponse().getStatus() < 300, text(r));
        return json(r, "$.id");
    }

    private String balance(String token, String boxId) throws Exception {
        return json(call(get("/api/v1/money-boxes/" + boxId), token), "$.balance");
    }

    private static String code(MvcResult r) throws Exception {
        return json(r, "$.code");
    }

    @Test
    void requiresAuthentication() throws Exception {
        assertEquals(401, mvc.perform(get("/api/v1/money-boxes?currency=BRL")).andReturn().getResponse().getStatus());
        assertEquals(401, mvc.perform(get("/api/v1/reserves/BRL/summary")).andReturn().getResponse().getStatus());
    }

    @Test
    void balanceIsDepositsMinusWithdrawalsAndCurrenciesAreSeparate() throws Exception {
        String token = newUserToken();
        String brl = createBox(token, "Reserva", "BRL", "2000");
        String eur = createBox(token, "Viagem", "EUR", null);

        ok(entry(token, brl, "DEPOSIT", "1000.00", "2026-10-01"));
        ok(entry(token, brl, "DEPOSIT", "500.00", "2026-10-02"));
        ok(entry(token, brl, "WITHDRAWAL", "300.00", "2026-10-03"));
        ok(entry(token, eur, "DEPOSIT", "400.00", "2026-10-03"));

        MvcResult list = call(get("/api/v1/money-boxes?currency=BRL"), token);
        assertEquals(200, list.getResponse().getStatus());
        assertEquals(1, ((List<?>) JsonPath.read(text(list), "$")).size());
        assertEquals("1200.00", json(list, "$[0].balance"));
        assertEquals("0.6000", json(list, "$[0].progress"));
        assertEquals("2000.00", json(list, "$[0].goal"));
        assertEquals("400.00", json(call(get("/api/v1/money-boxes?currency=EUR"), token), "$[0].balance"));
        assertEquals("0.0000", json(call(get("/api/v1/money-boxes?currency=EUR"), token), "$[0].progress"));
        assertEquals("1200.00", json(call(get("/api/v1/reserves/BRL/summary"), token), "$.total"));
        assertEquals("400.00", json(call(get("/api/v1/reserves/EUR/summary"), token), "$.total"));
    }

    @Test
    void listIsOrderedByCreationAndEntriesByDateDescending() throws Exception {
        String token = newUserToken();
        String first = createBox(token, "Primeiro", "BRL", null);
        createBox(token, "Segundo", "BRL", null);
        ok(entry(token, first, "DEPOSIT", "10.00", "2026-10-01"));
        ok(entry(token, first, "DEPOSIT", "20.00", "2026-10-05"));
        ok(entry(token, first, "DEPOSIT", "30.00", "2026-10-03"));

        MvcResult boxes = call(get("/api/v1/money-boxes?currency=BRL"), token);
        assertEquals("Primeiro", json(boxes, "$[0].name"));
        assertEquals("Segundo", json(boxes, "$[1].name"));
        assertEquals("0.00", json(boxes, "$[1].balance"));

        MvcResult entries = call(get("/api/v1/money-boxes/" + first + "/entries"), token);
        assertEquals("2026-10-05", json(entries, "$[0].date"));
        assertEquals("2026-10-03", json(entries, "$[1].date"));
        assertEquals("2026-10-01", json(entries, "$[2].date"));
        assertEquals("BRL", json(entries, "$[0].currency"));
    }

    @Test
    void withdrawalAboveBalanceOrWithZeroBalanceIsRefusedAndNothingIsSaved() throws Exception {
        String token = newUserToken();
        String box = createBox(token, "Reserva", "BRL", null);

        MvcResult zero = entry(token, box, "WITHDRAWAL", "1.00", "2026-10-01");
        assertEquals(409, zero.getResponse().getStatus());
        assertEquals("INSUFFICIENT_BALANCE", code(zero));

        ok(entry(token, box, "DEPOSIT", "1200.00", "2026-10-01"));
        MvcResult above = entry(token, box, "WITHDRAWAL", "1300.00", "2026-10-02");
        assertEquals(409, above.getResponse().getStatus());
        assertEquals("INSUFFICIENT_BALANCE", code(above));
        assertEquals("1200.00", balance(token, box));
        assertEquals(1, ((List<?>) JsonPath.read(text(call(get("/api/v1/money-boxes/" + box + "/entries"), token)), "$")).size());

        ok(entry(token, box, "WITHDRAWAL", "1200.00", "2026-10-03"));
        assertEquals("0.00", balance(token, box));
    }

    @Test
    void editOrDeleteThatWouldMakeTheBalanceNegativeIsRefusedAndHistoryIsKept() throws Exception {
        String token = newUserToken();
        String box = createBox(token, "Reserva", "BRL", null);
        String deposit = ok(entry(token, box, "DEPOSIT", "100.00", "2026-10-01"));
        String withdrawal = ok(entry(token, box, "WITHDRAWAL", "80.00", "2026-10-02"));
        String base = "/api/v1/money-boxes/" + box + "/entries/";

        MvcResult deleteDeposit = call(delete(base + deposit), token);
        assertEquals(409, deleteDeposit.getResponse().getStatus());
        assertEquals("INSUFFICIENT_BALANCE", code(deleteDeposit));

        MvcResult reduce = call(put(base + deposit).contentType(MediaType.APPLICATION_JSON)
            .content("{\"kind\":\"DEPOSIT\",\"amount\":\"50.00\",\"date\":\"2026-10-01\"}"), token);
        assertEquals(409, reduce.getResponse().getStatus());

        MvcResult toWithdrawal = call(put(base + deposit).contentType(MediaType.APPLICATION_JSON)
            .content("{\"kind\":\"WITHDRAWAL\",\"amount\":\"100.00\",\"date\":\"2026-10-01\"}"), token);
        assertEquals(409, toWithdrawal.getResponse().getStatus());
        assertEquals("20.00", balance(token, box));

        // editar o saque para baixo e trocar o tipo de forma segura são aceitos
        MvcResult lower = call(put(base + withdrawal).contentType(MediaType.APPLICATION_JSON)
            .content("{\"kind\":\"WITHDRAWAL\",\"amount\":\"30.00\",\"date\":\"2026-10-02\",\"note\":\"Ajuste\"}"), token);
        assertEquals(200, lower.getResponse().getStatus(), text(lower));
        assertEquals("Ajuste", json(lower, "$.note"));
        assertEquals("70.00", balance(token, box));
        assertEquals(204, call(delete(base + withdrawal), token).getResponse().getStatus());
        assertEquals("100.00", balance(token, box));
    }

    @Test
    void simultaneousWithdrawalsNeverExceedTheBalance() throws Exception {
        String token = newUserToken();
        String box = createBox(token, "Reserva", "BRL", null);
        ok(entry(token, box, "DEPOSIT", "100.00", "2026-10-01"));

        int threads = 8;
        ExecutorService pool = Executors.newFixedThreadPool(threads);
        CountDownLatch ready = new CountDownLatch(threads);
        CountDownLatch go = new CountDownLatch(1);
        List<Future<Integer>> results = new ArrayList<>();
        for (int i = 0; i < threads; i++) {
            Callable<Integer> task = () -> {
                ready.countDown();
                go.await();
                return entry(token, box, "WITHDRAWAL", "30.00", "2026-10-02").getResponse().getStatus();
            };
            results.add(pool.submit(task));
        }
        ready.await();
        go.countDown();
        int accepted = 0;
        int refused = 0;
        for (Future<Integer> result : results) {
            int status = result.get();
            if (status == 201) {
                accepted++;
            } else if (status == 409) {
                refused++;
            }
        }
        pool.shutdown();

        assertEquals(3, accepted, "100 / 30 = 3 saques cabem");
        assertEquals(threads - 3, refused);
        assertEquals("10.00", balance(token, box));
    }

    @Test
    void currencyIsInheritedAndMismatchOrChangeWithEntriesIsRefused() throws Exception {
        String token = newUserToken();
        String box = createBox(token, "Reserva", "BRL", null);

        MvcResult mismatch = call(post("/api/v1/money-boxes/" + box + "/entries").contentType(MediaType.APPLICATION_JSON)
            .content("{\"kind\":\"DEPOSIT\",\"amount\":\"10.00\",\"date\":\"2026-10-01\",\"currency\":\"EUR\"}"), token);
        assertEquals(409, mismatch.getResponse().getStatus());
        assertEquals("CURRENCY_MISMATCH", code(mismatch));

        String update = "{\"name\":\"Reserva\",\"icon\":\"piggy-bank\",\"color\":\"#22C55E\",\"currency\":\"EUR\"}";
        MvcResult freeChange = call(put("/api/v1/money-boxes/" + box).contentType(MediaType.APPLICATION_JSON).content(update), token);
        assertEquals(200, freeChange.getResponse().getStatus(), text(freeChange));
        assertEquals("EUR", json(freeChange, "$.currency"));

        ok(entry(token, box, "DEPOSIT", "10.00", "2026-10-01"));
        MvcResult locked = call(put("/api/v1/money-boxes/" + box).contentType(MediaType.APPLICATION_JSON)
            .content(update.replace("EUR", "BRL")), token);
        assertEquals(409, locked.getResponse().getStatus());
        assertEquals("CURRENCY_LOCKED", code(locked));

        MvcResult sameCurrency = call(put("/api/v1/money-boxes/" + box).contentType(MediaType.APPLICATION_JSON)
            .content(update.replace("Reserva", "Renomeado")), token);
        assertEquals(200, sameCurrency.getResponse().getStatus());
        assertEquals("10.00", json(sameCurrency, "$.balance"));
    }

    @Test
    void deletingTheBoxRemovesItsEntriesAndNothingElse() throws Exception {
        String token = newUserToken();
        String doomed = createBox(token, "Apagar", "BRL", null);
        String kept = createBox(token, "Manter", "BRL", null);
        ok(entry(token, doomed, "DEPOSIT", "10.00", "2026-10-01"));
        ok(entry(token, doomed, "DEPOSIT", "20.00", "2026-10-02"));
        ok(entry(token, kept, "DEPOSIT", "5.00", "2026-10-02"));

        assertEquals(204, call(delete("/api/v1/money-boxes/" + doomed), token).getResponse().getStatus());

        assertEquals(404, call(get("/api/v1/money-boxes/" + doomed), token).getResponse().getStatus());
        assertEquals(404, call(get("/api/v1/money-boxes/" + doomed + "/entries"), token).getResponse().getStatus());
        assertEquals(0, jdbc.queryForObject("SELECT count(*) FROM money_box_entry WHERE money_box_id = ?::uuid", Integer.class, doomed));
        assertEquals("5.00", balance(token, kept));
        assertEquals("5.00", json(call(get("/api/v1/reserves/BRL/summary"), token), "$.total"));
    }

    @Test
    void generalGoalIsIndependentAndZeroRemovesIt() throws Exception {
        String token = newUserToken();
        String box = createBox(token, "Reserva", "BRL", "100");
        ok(entry(token, box, "DEPOSIT", "250.00", "2026-10-01"));

        MvcResult none = call(get("/api/v1/reserves/BRL/summary"), token);
        assertEquals("250.00", json(none, "$.total"));
        assertEquals("0.0000", json(none, "$.progress"));
        assertEquals(1, (int) JsonPath.read(text(none), "$.boxCount"));
        assertTrue(((java.util.Map<?, ?>) JsonPath.read(text(none), "$")).get("goal") == null);

        MvcResult set = call(put("/api/v1/reserves/BRL/goal").contentType(MediaType.APPLICATION_JSON).content("{\"amount\":\"1000\"}"), token);
        assertEquals(200, set.getResponse().getStatus(), text(set));
        assertEquals("1000.00", json(set, "$.goal"));
        assertEquals("0.2500", json(set, "$.progress"));
        assertEquals("1.0000", json(call(get("/api/v1/money-boxes/" + box), token), "$.progress"));

        MvcResult update = call(put("/api/v1/reserves/BRL/goal").contentType(MediaType.APPLICATION_JSON).content("{\"amount\":\"200.00\"}"), token);
        assertEquals("1.0000", json(update, "$.progress"));

        MvcResult remove = call(put("/api/v1/reserves/BRL/goal").contentType(MediaType.APPLICATION_JSON).content("{\"amount\":\"0\"}"), token);
        assertTrue(((java.util.Map<?, ?>) JsonPath.read(text(remove), "$")).get("goal") == null);
        assertEquals("0.0000", json(remove, "$.progress"));
        assertEquals(0, jdbc.queryForObject("SELECT count(*) FROM reserve_goal", Integer.class));

        assertEquals("0.00", json(call(get("/api/v1/reserves/EUR/summary"), token), "$.total"));
    }

    @Test
    void invalidInputsAre400() throws Exception {
        String token = newUserToken();
        String box = createBox(token, "Reserva", "BRL", null);

        assertEquals(400, call(put("/api/v1/reserves/BRL/goal").contentType(MediaType.APPLICATION_JSON).content("{\"amount\":\"-1\"}"), token).getResponse().getStatus());
        assertEquals(400, call(put("/api/v1/reserves/BRL/goal").contentType(MediaType.APPLICATION_JSON).content("{}"), token).getResponse().getStatus());
        assertEquals(400, entry(token, box, "DEPOSIT", "0", "2026-10-01").getResponse().getStatus());
        assertEquals(400, entry(token, box, "DEPOSIT", "-5", "2026-10-01").getResponse().getStatus());
        assertEquals(400, entry(token, box, "DEPOSIT", "10", "1999-01-01").getResponse().getStatus());
        assertEquals(400, entry(token, box, "TRANSFER", "10", "2026-10-01").getResponse().getStatus());
        assertEquals(400, call(post("/api/v1/money-boxes").contentType(MediaType.APPLICATION_JSON)
            .content("{\"name\":\"\",\"icon\":\"x\",\"color\":\"#22C55E\",\"currency\":\"BRL\"}"), token).getResponse().getStatus());
        assertEquals(400, call(post("/api/v1/money-boxes").contentType(MediaType.APPLICATION_JSON)
            .content("{\"name\":\"A\",\"icon\":\"x\",\"color\":\"#22C55E\",\"currency\":\"BRL\",\"goal\":\"-10\"}"), token).getResponse().getStatus());
        assertEquals(400, call(get("/api/v1/money-boxes?currency=USD"), token).getResponse().getStatus());
    }

    @Test
    void anotherUserSeesAndChangesNothing() throws Exception {
        String owner = newUserToken();
        String stranger = newUserToken();
        String box = createBox(owner, "Privado", "BRL", "100");
        String entryId = ok(entry(owner, box, "DEPOSIT", "50.00", "2026-10-01"));
        String base = "/api/v1/money-boxes/" + box;

        assertEquals(404, call(get(base), stranger).getResponse().getStatus());
        assertEquals(404, call(get(base + "/entries"), stranger).getResponse().getStatus());
        assertEquals(404, entry(stranger, box, "DEPOSIT", "5.00", "2026-10-01").getResponse().getStatus());
        assertEquals(404, call(put(base).contentType(MediaType.APPLICATION_JSON)
            .content("{\"name\":\"X\",\"icon\":\"x\",\"color\":\"#22C55E\",\"currency\":\"BRL\"}"), stranger).getResponse().getStatus());
        assertEquals(404, call(put(base + "/entries/" + entryId).contentType(MediaType.APPLICATION_JSON)
            .content("{\"kind\":\"DEPOSIT\",\"amount\":\"1.00\",\"date\":\"2026-10-01\"}"), stranger).getResponse().getStatus());
        assertEquals(404, call(delete(base + "/entries/" + entryId), stranger).getResponse().getStatus());
        assertEquals(404, call(delete(base), stranger).getResponse().getStatus());
        assertEquals("[]", text(call(get("/api/v1/money-boxes?currency=BRL"), stranger)));
        assertEquals("0.00", json(call(get("/api/v1/reserves/BRL/summary"), stranger), "$.total"));
        assertEquals("50.00", balance(owner, box));
    }

    @Test
    void reservesTotalAppearsOnTheDashboardWithoutChangingMonthlyBalance() throws Exception {
        String token = newUserToken();
        String box = createBox(token, "Reserva", "BRL", null);
        ok(entry(token, box, "DEPOSIT", "700.00", "2026-10-05"));
        ok(entry(token, box, "WITHDRAWAL", "200.00", "2026-10-06"));

        MvcResult overview = call(get("/api/v1/overview?month=2026-10&currency=BRL"), token);

        assertEquals("500.00", json(overview, "$.reservesTotal"));
        assertEquals("0.00", json(overview, "$.summary.income"));
        assertEquals("0.00", json(overview, "$.summary.expense"));
        assertEquals("0.00", json(overview, "$.summary.balance"));
        assertEquals("0.00", json(call(get("/api/v1/overview?month=2026-10&currency=EUR"), token), "$.reservesTotal"));
    }
}
