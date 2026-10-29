package com.dowglasmaia.financecontroller.budget;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
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
import java.util.concurrent.Executors;
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
 * Budgets through the real HTTP stack and a real PostgreSQL (Testcontainers): save/get/progress, override and
 * "no goal this month", currency independence, atomic save, isolation, and the budget alert end to end through
 * transactions (including the threshold under concurrency).
 */
@AutoConfigureMockMvc
class BudgetFlowIntegrationTest extends AbstractPostgresIntegrationTest {
    private static final String PASSWORD = "Sup3r-Secret-Passw0rd";

    @DynamicPropertySource
    static void identityProperties(DynamicPropertyRegistry registry) {
        registry.add("app.identity.jwt.secret", () -> "test-only-dummy-jwt-secret-0123456789-abcdef");
    }

    @Autowired
    private MockMvc mvc;

    private String newUserToken() throws Exception {
        String email = "bud." + UUID.randomUUID() + "@example.com";
        MvcResult result = mvc.perform(post("/api/v1/auth/register").with(r -> {
            r.setRemoteAddr("10.7." + (int) (Math.random() * 250) + "." + (int) (Math.random() * 250));
            return r;
        }).contentType(MediaType.APPLICATION_JSON)
            .content("{\"email\":\"" + email + "\",\"password\":\"" + PASSWORD + "\"}")).andReturn();
        assertEquals(201, result.getResponse().getStatus());
        return JsonPath.read(text(result), "$.accessToken");
    }

    private MvcResult call(MockHttpServletRequestBuilder request, String token) throws Exception {
        return mvc.perform(request.header(HttpHeaders.AUTHORIZATION, "Bearer " + token)).andReturn();
    }

    private static MockHttpServletRequestBuilder json(MockHttpServletRequestBuilder request, String body) {
        return request.contentType(MediaType.APPLICATION_JSON).content(body);
    }

    private static String text(MvcResult r) throws Exception {
        return r.getResponse().getContentAsString();
    }

    private static String code(MvcResult r) throws Exception {
        return JsonPath.read(text(r), "$.code");
    }

    private String categoryId(String token, String name) throws Exception {
        List<String> ids = JsonPath.read(text(call(get("/api/v1/categories"), token)), "$[?(@.name=='" + name + "')].id");
        return ids.get(0);
    }

    private MvcResult save(String token, String currency, String body) throws Exception {
        return call(json(put("/api/v1/budgets/" + currency), body), token);
    }

    private MvcResult budget(String token, String currency, String month) throws Exception {
        return call(get("/api/v1/budgets/" + currency + "?month=" + month), token);
    }

    private MvcResult progress(String token, String currency, String month) throws Exception {
        return call(get("/api/v1/budgets/" + currency + "/progress?month=" + month), token);
    }

    private static String saveBody(String month, String monthly, String override, String categoryLimits) {
        return "{\"month\":\"" + month + "\""
            + (monthly == null ? "" : ",\"monthlyLimit\":\"" + monthly + "\"")
            + (override == null ? "" : ",\"monthOverride\":" + override)
            + ",\"categoryLimits\":" + categoryLimits + "}";
    }

    private MvcResult expense(String token, String categoryId, String amount, String date, String referenceMonth, String currency) throws Exception {
        return call(json(post("/api/v1/transactions"), "{\"kind\":\"EXPENSE\",\"description\":\"Gasto\",\"amount\":\"" + amount
            + "\",\"currency\":\"" + currency + "\",\"date\":\"" + date + "\",\"categoryId\":\"" + categoryId
            + "\",\"referenceMonth\":\"" + referenceMonth + "\"}"), token);
    }

    /** Campo de uma categoria no progresso (o filtro do JsonPath devolve uma lista). */
    private static String entry(MvcResult result, String categoryId, String field) throws Exception {
        List<String> values = JsonPath.read(text(result), "$.categories[?(@.categoryId=='" + categoryId + "')]." + field);
        return values.get(0);
    }

    private static String alertStatus(MvcResult r) throws Exception {
        String body = text(r);
        return body.contains("budgetAlert") ? JsonPath.read(body, "$.budgetAlert.status") : null;
    }

    private static String alertScope(MvcResult r) throws Exception {
        return JsonPath.read(text(r), "$.budgetAlert.scope");
    }

    @Test
    void requiresAuthentication() throws Exception {
        assertEquals(401, mvc.perform(get("/api/v1/budgets/BRL?month=2026-07")).andReturn().getResponse().getStatus());
    }

    @Test
    void newUserHasNoGoalAndSaveReturnsTheStoredConfiguration() throws Exception {
        String token = newUserToken();
        String food = categoryId(token, "Alimentação");

        MvcResult empty = budget(token, "BRL", "2026-07");
        assertEquals(200, empty.getResponse().getStatus());
        assertFalse(text(empty).contains("monthlyLimit"));
        assertFalse(text(empty).contains("effectiveLimit"));
        assertEquals(0, ((List<?>) JsonPath.read(text(empty), "$.categoryLimits")).size());

        MvcResult saved = save(token, "BRL", saveBody("2026-07", "1000", "{\"limit\":\"1500\"}",
            "[{\"categoryId\":\"" + food + "\",\"limit\":\"200.5\"}]"));
        assertEquals(200, saved.getResponse().getStatus());
        assertEquals("1000.00", JsonPath.read(text(saved), "$.monthlyLimit"));
        assertEquals("1500.00", JsonPath.read(text(saved), "$.monthOverride.limit"));
        assertEquals("1500.00", JsonPath.read(text(saved), "$.effectiveLimit"));
        assertEquals("200.50", JsonPath.read(text(saved), "$.categoryLimits[0].limit"));
        assertEquals(food, JsonPath.read(text(saved), "$.categoryLimits[0].categoryId"));

        // outro mês: sem ajuste, volta ao recorrente
        MvcResult august = budget(token, "BRL", "2026-08");
        assertEquals("1000.00", JsonPath.read(text(august), "$.effectiveLimit"));
        assertFalse(text(august).contains("monthOverride"));
    }

    @Test
    void savingWithoutOverrideRemovesTheMonthAdjustmentAndZeroRemovesLimits() throws Exception {
        String token = newUserToken();
        String food = categoryId(token, "Alimentação");
        save(token, "BRL", saveBody("2026-07", "1000", "{\"limit\":\"1500\"}", "[{\"categoryId\":\"" + food + "\",\"limit\":\"200\"}]"));

        MvcResult restored = save(token, "BRL", saveBody("2026-07", "1000", null, "[{\"categoryId\":\"" + food + "\",\"limit\":\"0\"}]"));
        assertEquals("1000.00", JsonPath.read(text(restored), "$.effectiveLimit"));
        assertFalse(text(restored).contains("monthOverride"));
        assertEquals(0, ((List<?>) JsonPath.read(text(restored), "$.categoryLimits")).size());

        MvcResult cleared = save(token, "BRL", saveBody("2026-07", "0", null, "[]"));
        assertFalse(text(cleared).contains("monthlyLimit"));
        assertFalse(text(budget(token, "BRL", "2026-07")).contains("effectiveLimit"));
    }

    @Test
    void disabledMonthHasNoGoalAndOtherMonthsKeepTheRecurringOne() throws Exception {
        String token = newUserToken();
        save(token, "BRL", saveBody("2026-07", "1000", "{\"disabled\":true}", "[]"));

        MvcResult july = budget(token, "BRL", "2026-07");
        assertTrue((boolean) JsonPath.read(text(july), "$.monthOverride.disabled"));
        assertFalse(text(july).contains("effectiveLimit"));
        assertEquals("NONE", JsonPath.read(text(progress(token, "BRL", "2026-07")), "$.global.status"));
        assertEquals("1000.00", JsonPath.read(text(budget(token, "BRL", "2026-08")), "$.effectiveLimit"));
    }

    @Test
    void currenciesAreIndependent() throws Exception {
        String token = newUserToken();
        save(token, "BRL", saveBody("2026-07", "1000", null, "[]"));
        save(token, "EUR", saveBody("2026-07", "50", null, "[]"));

        assertEquals("1000.00", JsonPath.read(text(budget(token, "BRL", "2026-07")), "$.monthlyLimit"));
        assertEquals("50.00", JsonPath.read(text(budget(token, "EUR", "2026-07")), "$.monthlyLimit"));
        assertEquals(400, call(get("/api/v1/budgets/USD?month=2026-07"), token).getResponse().getStatus());
    }

    @Test
    void invalidSaveFailsCompletelyAndKeepsThePreviousConfiguration() throws Exception {
        String token = newUserToken();
        String food = categoryId(token, "Alimentação");
        String salary = categoryId(token, "Salário");
        save(token, "BRL", saveBody("2026-07", "1000", null, "[{\"categoryId\":\"" + food + "\",\"limit\":\"200\"}]"));

        MvcResult income = save(token, "BRL", saveBody("2026-07", "9999", "{\"limit\":\"500\"}",
            "[{\"categoryId\":\"" + food + "\",\"limit\":\"300\"},{\"categoryId\":\"" + salary + "\",\"limit\":\"50\"}]"));
        assertEquals(409, income.getResponse().getStatus());
        assertEquals("CATEGORY_TYPE_MISMATCH", code(income));

        MvcResult unknown = save(token, "BRL", saveBody("2026-07", "9999", null,
            "[{\"categoryId\":\"" + UUID.randomUUID() + "\",\"limit\":\"50\"}]"));
        assertEquals(404, unknown.getResponse().getStatus());

        for (String bad : new String[] {
            saveBody("2026-07", "-1", null, "[]"),
            saveBody("2026-07", "abc", null, "[]"),
            saveBody("2026-07", "1000", "{\"limit\":\"10\",\"disabled\":true}", "[]"),
            saveBody("2026-13", "1000", null, "[]"),
            saveBody("2026-07", "1000", null, "[{\"categoryId\":\"" + food + "\",\"limit\":\"1\"},{\"categoryId\":\"" + food + "\",\"limit\":\"2\"}]")}) {
            assertEquals(400, save(token, "BRL", bad).getResponse().getStatus(), bad);
        }

        MvcResult unchanged = budget(token, "BRL", "2026-07");
        assertEquals("1000.00", JsonPath.read(text(unchanged), "$.monthlyLimit"));
        assertEquals("200.00", JsonPath.read(text(unchanged), "$.categoryLimits[0].limit"));
        assertFalse(text(unchanged).contains("monthOverride"));
    }

    @Test
    void usersNeverSeeEachOthersBudgetsOrUseEachOthersCategories() throws Exception {
        String ana = newUserToken();
        String rui = newUserToken();
        String anaFood = categoryId(ana, "Alimentação");
        save(ana, "BRL", saveBody("2026-07", "1000", null, "[{\"categoryId\":\"" + anaFood + "\",\"limit\":\"200\"}]"));

        MvcResult ruiBudget = budget(rui, "BRL", "2026-07");
        assertFalse(text(ruiBudget).contains("monthlyLimit"));
        assertEquals(0, ((List<?>) JsonPath.read(text(ruiBudget), "$.categoryLimits")).size());
        assertEquals("NONE", JsonPath.read(text(progress(rui, "BRL", "2026-07")), "$.global.status"));
        assertEquals(404, save(rui, "BRL", saveBody("2026-07", null, null, "[{\"categoryId\":\"" + anaFood + "\",\"limit\":\"5\"}]")).getResponse().getStatus());
        assertEquals("1000.00", JsonPath.read(text(budget(ana, "BRL", "2026-07")), "$.monthlyLimit"));
    }

    @Test
    void progressShowsExactSpentFractionRemainingAndStatusPerCategoryAndGlobal() throws Exception {
        String token = newUserToken();
        String food = categoryId(token, "Alimentação");
        String car = categoryId(token, "Carro");
        save(token, "BRL", saveBody("2026-07", "1000", null, "[{\"categoryId\":\"" + food + "\",\"limit\":\"200\"}]"));
        expense(token, food, "50.00", "2026-07-03", "2026-07", "BRL");
        expense(token, car, "30.25", "2026-07-04", "2026-07", "BRL");
        expense(token, food, "999.00", "2026-08-01", "2026-08", "BRL");
        expense(token, food, "777.00", "2026-07-05", "2026-07", "EUR");
        call(json(post("/api/v1/transactions"), "{\"kind\":\"INCOME\",\"description\":\"Pago\",\"amount\":\"5000.00\",\"currency\":\"BRL\","
            + "\"date\":\"2026-07-02\",\"categoryId\":\"" + categoryId(token, "Salário") + "\"}"), token);

        MvcResult result = progress(token, "BRL", "2026-07");

        assertEquals(200, result.getResponse().getStatus());
        assertEquals("80.25", JsonPath.read(text(result), "$.global.spent"));
        assertEquals("1000.00", JsonPath.read(text(result), "$.global.limit"));
        assertEquals("OK", JsonPath.read(text(result), "$.global.status"));
        assertEquals("0.0803", JsonPath.read(text(result), "$.global.fraction"));
        assertEquals("919.75", JsonPath.read(text(result), "$.global.remaining"));
        List<String> names = JsonPath.read(text(call(get("/api/v1/categories?type=EXPENSE"), token)), "$[*].id");
        List<String> listed = JsonPath.read(text(result), "$.categories[*].categoryId");
        assertEquals(names.size(), listed.size(), "todas as categorias que aceitam despesa");
        assertFalse(listed.contains(categoryId(token, "Salário")));
        assertEquals("50.00", entry(result, food, "spent"));
        assertEquals("0.2500", entry(result, food, "fraction"));
        assertEquals("150.00", entry(result, food, "remaining"));
        assertEquals("NONE", entry(result, car, "status"));
        assertEquals("30.25", entry(result, car, "spent"));
    }

    @Test
    void alertGoesFromOkToWarningToExceededAndNeverRepeats() throws Exception {
        String token = newUserToken();
        String food = categoryId(token, "Alimentação");
        save(token, "BRL", saveBody("2026-07", "1000", null, "[]"));

        assertEquals(null, alertStatus(expense(token, food, "700.00", "2026-07-02", "2026-07", "BRL")));
        MvcResult warn = expense(token, food, "150.00", "2026-07-03", "2026-07", "BRL");
        assertEquals("WARNING", alertStatus(warn));
        assertEquals("GLOBAL", alertScope(warn));
        MvcResult exceeded = expense(token, food, "250.00", "2026-07-04", "2026-07", "BRL");
        assertEquals("EXCEEDED", alertStatus(exceeded));
        assertEquals(null, alertStatus(expense(token, food, "200.00", "2026-07-05", "2026-07", "BRL")));
    }

    @Test
    void categoryAlertHasPriorityAndNamesTheCategory() throws Exception {
        String token = newUserToken();
        String leisure = categoryId(token, "Lazer");
        save(token, "BRL", saveBody("2026-07", "1000", null, "[{\"categoryId\":\"" + leisure + "\",\"limit\":\"200\"}]"));

        MvcResult result = expense(token, leisure, "170.00", "2026-07-02", "2026-07", "BRL");

        assertEquals("CATEGORY", alertScope(result));
        assertEquals("WARNING", alertStatus(result));
        assertEquals("Lazer", JsonPath.read(text(result), "$.budgetAlert.categoryName"));
        assertEquals(leisure, JsonPath.read(text(result), "$.budgetAlert.categoryId"));
    }

    @Test
    void editingAnExpenseDoesNotCountItsOldValueAndImprovingNeverAlerts() throws Exception {
        String token = newUserToken();
        String food = categoryId(token, "Alimentação");
        save(token, "BRL", saveBody("2026-07", "1000", null, "[]"));
        String id = JsonPath.read(text(expense(token, food, "700.00", "2026-07-02", "2026-07", "BRL")), "$.transaction.id");

        // 700 -> 850 (e não 1550): WARNING
        String body = "{\"kind\":\"EXPENSE\",\"description\":\"Gasto\",\"amount\":\"850.00\",\"currency\":\"BRL\",\"date\":\"2026-07-02\","
            + "\"categoryId\":\"" + food + "\",\"referenceMonth\":\"2026-07\"}";
        MvcResult edited = call(json(put("/api/v1/transactions/" + id), body), token);
        assertEquals("WARNING", alertStatus(edited));

        // 850 -> 100: melhora, sem alerta
        MvcResult lower = call(json(put("/api/v1/transactions/" + id), body.replace("850.00", "100.00")), token);
        assertEquals(null, alertStatus(lower));
    }

    @Test
    void noAlertForIncomeForExpensesOutsideTheReferenceMonthOrWithoutGoalOrInAnotherCurrency() throws Exception {
        String token = newUserToken();
        String food = categoryId(token, "Alimentação");
        String salary = categoryId(token, "Salário");
        save(token, "BRL", saveBody("2026-07", "100", null, "[]"));

        MvcResult income = call(json(post("/api/v1/transactions"), "{\"kind\":\"INCOME\",\"description\":\"x\",\"amount\":\"5000.00\",\"currency\":\"BRL\","
            + "\"date\":\"2026-07-02\",\"categoryId\":\"" + salary + "\",\"referenceMonth\":\"2026-07\"}"), token);
        assertEquals(null, alertStatus(income));
        // despesa datada em agosto enquanto o cliente exibe julho: sem alerta
        assertEquals(null, alertStatus(expense(token, food, "500.00", "2026-08-02", "2026-07", "BRL")));
        // EUR não tem meta
        assertEquals(null, alertStatus(expense(token, food, "500.00", "2026-07-02", "2026-07", "EUR")));
        // sem referenceMonth: o servidor não avalia
        MvcResult noReference = call(json(post("/api/v1/transactions"), "{\"kind\":\"EXPENSE\",\"description\":\"x\",\"amount\":\"10.00\",\"currency\":\"BRL\","
            + "\"date\":\"2026-07-02\",\"categoryId\":\"" + food + "\"}"), token);
        assertEquals(null, alertStatus(noReference));
        // a mesma despesa dentro do mês e da moeda com meta alerta
        assertEquals("EXCEEDED", alertStatus(expense(token, food, "500.00", "2026-07-03", "2026-07", "BRL")));
    }

    @Test
    void disabledMonthSilencesAlertsOnlyForThatMonth() throws Exception {
        String token = newUserToken();
        String food = categoryId(token, "Alimentação");
        save(token, "BRL", saveBody("2026-07", "100", "{\"disabled\":true}", "[]"));

        assertEquals(null, alertStatus(expense(token, food, "500.00", "2026-07-02", "2026-07", "BRL")));
        assertEquals("EXCEEDED", alertStatus(expense(token, food, "500.00", "2026-08-02", "2026-08", "BRL")));
    }

    @Test
    void twoSimultaneousExpensesAtTheThresholdAlertExactlyOnce() throws Exception {
        String token = newUserToken();
        String food = categoryId(token, "Alimentação");
        save(token, "BRL", saveBody("2026-07", "1000", null, "[]"));
        expense(token, food, "700.00", "2026-07-02", "2026-07", "BRL");

        var start = new CountDownLatch(1);
        var pool = Executors.newFixedThreadPool(4);
        Callable<String> task = () -> {
            start.await();
            return alertStatus(expense(token, food, "60.00", "2026-07-03", "2026-07", "BRL"));
        };
        var futures = new ArrayList<java.util.concurrent.Future<String>>();
        for (int i = 0; i < 4; i++) {
            futures.add(pool.submit(task));
        }
        start.countDown();
        List<String> alerts = new ArrayList<>();
        for (var f : futures) {
            alerts.add(f.get());
        }
        pool.shutdown();

        // 700 -> 760 -> 820 (WARNING) -> 880 -> 940: o limiar de 80% é cruzado por exatamente uma delas
        assertEquals(1, alerts.stream().filter("WARNING"::equals).count(), alerts.toString());
        assertEquals(0, alerts.stream().filter("EXCEEDED"::equals).count(), alerts.toString());
    }

    @Test
    void concurrentSavesOfTheSameBudgetNeverFailAndKeepAConsistentSet() throws Exception {
        String token = newUserToken();
        String food = categoryId(token, "Alimentação");
        String leisure = categoryId(token, "Lazer");
        String body = saveBody("2026-07", "1000", null,
            "[{\"categoryId\":\"" + food + "\",\"limit\":\"200\"},{\"categoryId\":\"" + leisure + "\",\"limit\":\"100\"}]");

        var start = new CountDownLatch(1);
        var pool = Executors.newFixedThreadPool(4);
        Callable<Integer> task = () -> {
            start.await();
            return save(token, "BRL", body).getResponse().getStatus();
        };
        var futures = new ArrayList<java.util.concurrent.Future<Integer>>();
        for (int i = 0; i < 4; i++) {
            futures.add(pool.submit(task));
        }
        start.countDown();
        for (var f : futures) {
            assertEquals(200, f.get());
        }
        pool.shutdown();

        assertEquals(2, ((List<?>) JsonPath.read(text(budget(token, "BRL", "2026-07")), "$.categoryLimits")).size());
    }

    @Test
    void deletingACategoryRemovesItsLimit() throws Exception {
        String token = newUserToken();
        String car = categoryId(token, "Carro");
        save(token, "BRL", saveBody("2026-07", null, null, "[{\"categoryId\":\"" + car + "\",\"limit\":\"300\"}]"));

        assertEquals(204, call(delete("/api/v1/categories/" + car), token).getResponse().getStatus());

        assertEquals(0, ((List<?>) JsonPath.read(text(budget(token, "BRL", "2026-07")), "$.categoryLimits")).size());
    }
}
