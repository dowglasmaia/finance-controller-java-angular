package com.dowglasmaia.financecontroller.transaction;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;

import com.dowglasmaia.financecontroller.support.AbstractPostgresIntegrationTest;
import com.jayway.jsonpath.JsonPath;
import java.math.BigDecimal;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
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
 * What the API leaves on disk: incomes and expenses are read back from {@code transaction_entry} with plain SQL
 * (kind, exact amount, currency, date, owner, category), an edit never creates a second row, a refused request
 * leaves nothing, the listing totals equal the SQL sums, and concurrent expenses of one month are all stored.
 */
@AutoConfigureMockMvc
class TransactionPersistenceIntegrationTest extends AbstractPostgresIntegrationTest {
    private static final String PASSWORD = "Sup3r-Secret-Passw0rd";

    @DynamicPropertySource
    static void identityProperties(DynamicPropertyRegistry registry) {
        registry.add("app.identity.jwt.secret", () -> "test-only-dummy-jwt-secret-0123456789-abcdef");
    }

    @Autowired
    private MockMvc mvc;

    @Autowired
    private JdbcTemplate jdbc;

    private record User(String token, UUID id) {
    }

    private User newUser() throws Exception {
        String email = "persist." + UUID.randomUUID() + "@example.com";
        MvcResult result = mvc.perform(post("/api/v1/auth/register").with(r -> {
            r.setRemoteAddr("10.9." + (int) (Math.random() * 250) + "." + (int) (Math.random() * 250));
            return r;
        }).contentType(MediaType.APPLICATION_JSON)
            .content("{\"email\":\"" + email + "\",\"password\":\"" + PASSWORD + "\"}")).andReturn();
        assertEquals(201, result.getResponse().getStatus());
        UUID id = jdbc.queryForObject("select id from app_user where email = ?", UUID.class, email);
        return new User(JsonPath.read(text(result), "$.accessToken"), id);
    }

    private MvcResult call(MockHttpServletRequestBuilder request, String token) throws Exception {
        return mvc.perform(request.header(HttpHeaders.AUTHORIZATION, "Bearer " + token)).andReturn();
    }

    private static String text(MvcResult r) throws Exception {
        return r.getResponse().getContentAsString();
    }

    private String categoryId(String token, String name) throws Exception {
        List<String> ids = JsonPath.read(text(call(get("/api/v1/categories"), token)), "$[?(@.name=='" + name + "')].id");
        return ids.get(0);
    }

    private static String body(String kind, String amount, String currency, String date, String categoryId) {
        return "{\"kind\":\"" + kind + "\",\"description\":\"Compra\",\"amount\":\"" + amount + "\",\"currency\":\""
            + currency + "\",\"date\":\"" + date + "\",\"categoryId\":\"" + categoryId + "\"}";
    }

    private MvcResult create(String token, String json) throws Exception {
        return call(post("/api/v1/transactions").contentType(MediaType.APPLICATION_JSON).content(json), token);
    }

    private int rows(UUID owner) {
        return jdbc.queryForObject("select count(*) from transaction_entry where owner_id = ?", Integer.class, owner);
    }

    @Test
    void incomeAndExpenseAreStoredWithTheExpectedColumns() throws Exception {
        User user = newUser();
        String salary = categoryId(user.token(), "Salário");
        String food = categoryId(user.token(), "Alimentação");

        String incomeId = JsonPath.read(text(create(user.token(), body("INCOME", "2700", "EUR", "2026-10-29", salary))),
            "$.transaction.id");
        String expenseId = JsonPath.read(text(create(user.token(), body("EXPENSE", "81.5", "BRL", "2026-10-31", food))),
            "$.transaction.id");

        Map<String, Object> income = jdbc.queryForMap("select * from transaction_entry where id = ?", UUID.fromString(incomeId));
        assertEquals("INCOME", income.get("kind"));
        assertEquals(new BigDecimal("2700.00"), income.get("amount"));
        assertEquals("EUR", income.get("currency"));
        assertEquals("2026-10-29", income.get("entry_date").toString());
        assertEquals(user.id(), income.get("owner_id"));
        assertEquals(UUID.fromString(salary), income.get("category_id"));

        Map<String, Object> expense = jdbc.queryForMap("select * from transaction_entry where id = ?", UUID.fromString(expenseId));
        assertEquals("EXPENSE", expense.get("kind"));
        assertEquals(new BigDecimal("81.50"), expense.get("amount"));
        assertEquals("BRL", expense.get("currency"));
        assertEquals(user.id(), expense.get("owner_id"));
        assertEquals(UUID.fromString(food), expense.get("category_id"));
        assertEquals(expense.get("created_at"), expense.get("updated_at"));
        assertEquals(2, rows(user.id()));
    }

    @Test
    void editUpdatesTheSameRowAndDeleteRemovesIt() throws Exception {
        User user = newUser();
        String food = categoryId(user.token(), "Alimentação");
        String leisure = categoryId(user.token(), "Lazer");
        MvcResult created = create(user.token(), body("EXPENSE", "10.00", "BRL", "2026-10-05", food));
        String id = JsonPath.read(text(created), "$.transaction.id");
        Thread.sleep(15);

        MvcResult edited = call(put("/api/v1/transactions/" + id).contentType(MediaType.APPLICATION_JSON)
            .content(body("EXPENSE", "99.90", "EUR", "2026-11-02", leisure)), user.token());

        assertEquals(200, edited.getResponse().getStatus());
        assertEquals(1, rows(user.id()));
        Map<String, Object> row = jdbc.queryForMap("select * from transaction_entry where id = ?", UUID.fromString(id));
        assertEquals(new BigDecimal("99.90"), row.get("amount"));
        assertEquals("EUR", row.get("currency"));
        assertEquals(UUID.fromString(leisure), row.get("category_id"));
        assertTrue(((Timestamp) row.get("updated_at")).after((Timestamp) row.get("created_at")));

        assertEquals(204, call(delete("/api/v1/transactions/" + id), user.token()).getResponse().getStatus());
        assertEquals(0, rows(user.id()));
    }

    @Test
    void anotherUserCannotChangeOrRemoveTheRow() throws Exception {
        User ana = newUser();
        User bia = newUser();
        String id = JsonPath.read(text(create(ana.token(),
            body("EXPENSE", "10.00", "BRL", "2026-10-05", categoryId(ana.token(), "Alimentação")))), "$.transaction.id");

        MvcResult edit = call(put("/api/v1/transactions/" + id).contentType(MediaType.APPLICATION_JSON)
            .content(body("EXPENSE", "1.00", "BRL", "2026-10-05", categoryId(bia.token(), "Alimentação"))), bia.token());
        MvcResult remove = call(delete("/api/v1/transactions/" + id), bia.token());

        assertEquals(404, edit.getResponse().getStatus());
        assertEquals(404, remove.getResponse().getStatus());
        assertEquals(new BigDecimal("10.00"),
            jdbc.queryForObject("select amount from transaction_entry where id = ?", BigDecimal.class, UUID.fromString(id)));
        assertEquals(0, rows(bia.id()));
    }

    @Test
    void refusedRequestsLeaveNoRow() throws Exception {
        User user = newUser();
        String food = categoryId(user.token(), "Alimentação");

        // entrada numa categoria só de saída, valor inválido e categoria de outro utilizador
        assertEquals(409, create(user.token(), body("INCOME", "10.00", "BRL", "2026-10-05", food)).getResponse().getStatus());
        assertEquals(400, create(user.token(), body("EXPENSE", "0", "BRL", "2026-10-05", food)).getResponse().getStatus());
        assertEquals(400, create(user.token(), body("EXPENSE", "10,50", "BRL", "2026-10-05", food)).getResponse().getStatus());
        assertEquals(404, create(user.token(),
            body("EXPENSE", "10.00", "BRL", "2026-10-05", UUID.randomUUID().toString())).getResponse().getStatus());

        assertEquals(0, rows(user.id()));
    }

    @Test
    void listingTotalsEqualTheSqlSumsPerKindAndCurrency() throws Exception {
        User user = newUser();
        String salary = categoryId(user.token(), "Salário");
        String food = categoryId(user.token(), "Alimentação");
        create(user.token(), body("INCOME", "2623.00", "EUR", "2026-10-29", salary));
        create(user.token(), body("INCOME", "2700.00", "EUR", "2026-10-31", salary));
        create(user.token(), body("EXPENSE", "81.51", "EUR", "2026-10-31", food));
        create(user.token(), body("EXPENSE", "87.51", "EUR", "2026-10-31", food));
        create(user.token(), body("EXPENSE", "35.00", "BRL", "2026-10-29", food));

        MvcResult eur = call(get("/api/v1/transactions?month=2026-10&currency=EUR"), user.token());

        assertEquals(sql(user.id(), "INCOME", "EUR"), new BigDecimal((String) JsonPath.read(text(eur), "$.totals.income")));
        assertEquals(sql(user.id(), "EXPENSE", "EUR"), new BigDecimal((String) JsonPath.read(text(eur), "$.totals.expense")));
        assertEquals(new BigDecimal("5323.00"), sql(user.id(), "INCOME", "EUR"));
        assertEquals(new BigDecimal("169.02"), sql(user.id(), "EXPENSE", "EUR"));
        assertEquals(new BigDecimal("35.00"), sql(user.id(), "EXPENSE", "BRL"));
    }

    @Test
    void concurrentExpensesOfTheSameMonthAreAllStored() throws Exception {
        User user = newUser();
        String food = categoryId(user.token(), "Alimentação");
        int threads = 12;
        ExecutorService pool = Executors.newFixedThreadPool(threads);
        CountDownLatch start = new CountDownLatch(1);
        List<Future<Integer>> results = new ArrayList<>();
        try {
            for (int i = 0; i < threads; i++) {
                Callable<Integer> task = () -> {
                    start.await();
                    return create(user.token(), "{\"kind\":\"EXPENSE\",\"description\":\"Compra\",\"amount\":\"10.01\","
                        + "\"currency\":\"EUR\",\"date\":\"2026-10-15\",\"categoryId\":\"" + food
                        + "\",\"referenceMonth\":\"2026-10\"}").getResponse().getStatus();
                };
                results.add(pool.submit(task));
            }
            start.countDown();
            for (Future<Integer> result : results) {
                assertEquals(201, result.get(30, TimeUnit.SECONDS));
            }
        } finally {
            pool.shutdownNow();
        }

        assertEquals(threads, rows(user.id()));
        assertEquals(new BigDecimal("120.12"), sql(user.id(), "EXPENSE", "EUR"));
    }

    private BigDecimal sql(UUID owner, String kind, String currency) {
        BigDecimal sum = jdbc.queryForObject("select coalesce(sum(amount), 0) from transaction_entry "
            + "where owner_id = ? and kind = ? and currency = ?", BigDecimal.class, owner, kind, currency);
        return sum.setScale(2);
    }
}
