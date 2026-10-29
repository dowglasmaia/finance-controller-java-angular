package com.dowglasmaia.financecontroller.report;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;

import com.dowglasmaia.financecontroller.support.AbstractPostgresIntegrationTest;
import com.jayway.jsonpath.JsonPath;
import jakarta.persistence.EntityManagerFactory;
import java.util.List;
import java.util.UUID;
import org.hibernate.SessionFactory;
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
 * The monthly overview through the real HTTP stack and a real PostgreSQL (Testcontainers): totals, variation, bands,
 * expense shares, latest transactions, budget progress, isolation between users and currencies, and the number of SQL
 * statements per call.
 */
@AutoConfigureMockMvc
class OverviewFlowIntegrationTest extends AbstractPostgresIntegrationTest {
    private static final String PASSWORD = "Sup3r-Secret-Passw0rd";

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
        registry.add("app.identity.jwt.secret", () -> "test-only-dummy-jwt-secret-0123456789-abcdef");
        registry.add("spring.jpa.properties.hibernate.generate_statistics", () -> "true");
    }

    @Autowired
    private MockMvc mvc;

    @Autowired
    private EntityManagerFactory entityManagerFactory;

    private String newUserToken() throws Exception {
        String email = "ovw." + UUID.randomUUID() + "@example.com";
        MvcResult result = mvc.perform(post("/api/v1/auth/register").with(r -> {
            r.setRemoteAddr("10.6." + (int) (Math.random() * 250) + "." + (int) (Math.random() * 250));
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

    private String categoryId(String token, String name) throws Exception {
        List<String> ids = JsonPath.read(text(call(get("/api/v1/categories"), token)), "$[?(@.name=='" + name + "')].id");
        return ids.get(0);
    }

    private void add(String token, String kind, String amount, String date, String categoryId, String currency) throws Exception {
        MvcResult r = call(post("/api/v1/transactions").contentType(MediaType.APPLICATION_JSON)
            .content("{\"kind\":\"" + kind + "\",\"description\":\"" + kind.toLowerCase() + " " + amount + "\",\"amount\":\"" + amount
                + "\",\"currency\":\"" + currency + "\",\"date\":\"" + date + "\",\"categoryId\":\"" + categoryId + "\"}"), token);
        assertEquals(201, r.getResponse().getStatus(), text(r));
    }

    private MvcResult overview(String token, String month, String currency) throws Exception {
        return call(get("/api/v1/overview?month=" + month + "&currency=" + currency), token);
    }

    private static String field(MvcResult r, String path) throws Exception {
        return JsonPath.read(text(r), path);
    }

    @Test
    void requiresAuthentication() throws Exception {
        assertEquals(401, mvc.perform(get("/api/v1/overview?month=2026-10&currency=BRL")).andReturn().getResponse().getStatus());
    }

    @Test
    void invalidParametersAre400() throws Exception {
        String token = newUserToken();

        assertEquals(400, call(get("/api/v1/overview?month=2026-13&currency=BRL"), token).getResponse().getStatus());
        assertEquals(400, call(get("/api/v1/overview?month=2026-10&currency=USD"), token).getResponse().getStatus());
        assertEquals(400, call(get("/api/v1/overview?currency=BRL"), token).getResponse().getStatus());
        assertEquals(400, call(get("/api/v1/overview?month=2026-10"), token).getResponse().getStatus());
    }

    @Test
    void emptyMonthIsZerosAndEmptyListsNotAnError() throws Exception {
        String token = newUserToken();

        MvcResult result = overview(token, "2030-05", "BRL");

        assertEquals(200, result.getResponse().getStatus());
        assertEquals("0.00", field(result, "$.summary.income"));
        assertEquals("0.00", field(result, "$.summary.expense"));
        assertEquals("0.00", field(result, "$.summary.balance"));
        assertEquals("0.00", field(result, "$.previous.balance"));
        assertEquals("0.00", field(result, "$.reservesTotal"));
        assertEquals(5, ((List<?>) JsonPath.read(text(result), "$.bars")).size());
        assertEquals(0, ((List<?>) JsonPath.read(text(result), "$.expenseByCategory")).size());
        assertEquals(0, ((List<?>) JsonPath.read(text(result), "$.latestTransactions")).size());
        assertEquals("NONE", field(result, "$.budgetProgress.status"));
        assertFalse(text(result).contains("incomeDeltaPercent"));
        assertFalse(text(result).contains("expenseDeltaPercent"));
    }

    @Test
    void summaryIsPerMonthAndCurrencyAndBalanceIsIncomeMinusExpense() throws Exception {
        String token = newUserToken();
        String salary = categoryId(token, "Salário");
        String food = categoryId(token, "Alimentação");
        add(token, "INCOME", "1000.00", "2026-10-02", salary, "BRL");
        add(token, "EXPENSE", "250.00", "2026-10-03", food, "BRL");
        add(token, "INCOME", "5000.00", "2026-10-02", salary, "EUR");
        add(token, "EXPENSE", "999.00", "2026-09-30", food, "BRL");
        add(token, "EXPENSE", "777.00", "2026-11-01", food, "BRL");

        MvcResult brl = overview(token, "2026-10", "BRL");
        assertEquals("1000.00", field(brl, "$.summary.income"));
        assertEquals("250.00", field(brl, "$.summary.expense"));
        assertEquals("750.00", field(brl, "$.summary.balance"));
        assertEquals("0.00", field(brl, "$.previous.income"));
        assertEquals("999.00", field(brl, "$.previous.expense"));
        assertEquals("-999.00", field(brl, "$.previous.balance"));

        MvcResult eur = overview(token, "2026-10", "EUR");
        assertEquals("5000.00", field(eur, "$.summary.income"));
        assertEquals("0.00", field(eur, "$.summary.expense"));
        assertEquals("5000.00", field(eur, "$.summary.balance"));
    }

    @Test
    void variationUsesThePreviousMonthAsBaseAndIsAbsentWithoutIt() throws Exception {
        String token = newUserToken();
        String salary = categoryId(token, "Salário");
        String food = categoryId(token, "Alimentação");
        add(token, "INCOME", "1000.00", "2026-09-10", salary, "BRL");
        add(token, "EXPENSE", "200.00", "2026-09-10", food, "BRL");
        add(token, "INCOME", "1250.00", "2026-10-10", salary, "BRL");
        add(token, "EXPENSE", "200.50", "2026-10-10", food, "BRL");

        MvcResult october = overview(token, "2026-10", "BRL");
        assertEquals("25.0", field(october, "$.incomeDeltaPercent"));
        assertEquals("0", field(october, "$.expenseDeltaPercent"));

        // setembro não tem base (agosto vazio): sem variação
        MvcResult september = overview(token, "2026-09", "BRL");
        assertFalse(text(september).contains("incomeDeltaPercent"));
        assertFalse(text(september).contains("expenseDeltaPercent"));
    }

    @Test
    void reservesTotalDoesNotDependOnTheMonth() throws Exception {
        String token = newUserToken();

        assertEquals(field(overview(token, "2026-10", "BRL"), "$.reservesTotal"), field(overview(token, "2020-01", "BRL"), "$.reservesTotal"));
    }

    @Test
    void barsAreSevenDayBandsAndLeapDayFallsInS5() throws Exception {
        String token = newUserToken();
        String salary = categoryId(token, "Salário");
        add(token, "INCOME", "10.00", "2028-02-01", salary, "BRL");
        add(token, "INCOME", "20.00", "2028-02-28", salary, "BRL");
        add(token, "INCOME", "30.00", "2028-02-29", salary, "BRL");
        add(token, "INCOME", "40.00", "2028-03-01", salary, "BRL");

        MvcResult february = overview(token, "2028-02", "BRL");

        assertEquals(List.of("S1", "S2", "S3", "S4", "S5"), JsonPath.read(text(february), "$.bars[*].label"));
        assertEquals("10.00", field(february, "$.bars[0].income"));
        assertEquals("20.00", field(february, "$.bars[3].income"));
        assertEquals("30.00", field(february, "$.bars[4].income"));
        assertEquals(4, ((List<?>) JsonPath.read(text(overview(token, "2026-02", "BRL")), "$.bars")).size());
        // fronteira de mês: 01/03 só entra em março
        assertEquals("40.00", field(overview(token, "2028-03", "BRL"), "$.bars[0].income"));
    }

    @Test
    void expenseByCategoryIsSortedBiggestFirstWithExactPercentages() throws Exception {
        String token = newUserToken();
        String food = categoryId(token, "Alimentação");
        String car = categoryId(token, "Carro");
        String leisure = categoryId(token, "Lazer");
        add(token, "EXPENSE", "100.00", "2026-10-02", car, "BRL");
        add(token, "EXPENSE", "100.00", "2026-10-02", leisure, "BRL");
        add(token, "EXPENSE", "100.00", "2026-10-03", food, "BRL");
        add(token, "INCOME", "5000.00", "2026-10-03", categoryId(token, "Salário"), "BRL");
        add(token, "EXPENSE", "500.00", "2026-10-03", food, "EUR");

        MvcResult result = overview(token, "2026-10", "BRL");

        List<String> names = JsonPath.read(text(result), "$.expenseByCategory[*].category.name");
        assertEquals(List.of("Alimentação", "Carro", "Lazer"), names, "empate resolvido pelo nome");
        assertEquals("33.3333", field(result, "$.expenseByCategory[0].percentage"));
        assertEquals("100.00", field(result, "$.expenseByCategory[0].amount"));
        assertEquals(food, field(result, "$.expenseByCategory[0].category.id"));
        assertTrue(text(result).contains("\"color\""));
    }

    @Test
    void latestTransactionsAreAtMostEightOfTheMonthAndCurrencyNewestFirst() throws Exception {
        String token = newUserToken();
        String food = categoryId(token, "Alimentação");
        List<String> bakeryIds = JsonPath.read(text(call(get("/api/v1/categories"), token)),
            "$[?(@.name=='Alimentação')].subcategories[?(@.name=='Padaria')].id");
        String bakery = bakeryIds.get(0);
        for (int day = 1; day <= 12; day++) {
            add(token, "EXPENSE", "1.00", "2026-10-" + String.format("%02d", day), food, "BRL");
        }
        add(token, "EXPENSE", "2.00", "2026-09-30", food, "BRL");
        add(token, "EXPENSE", "3.00", "2026-10-20", food, "EUR");
        MvcResult withSub = call(post("/api/v1/transactions").contentType(MediaType.APPLICATION_JSON)
            .content("{\"kind\":\"EXPENSE\",\"description\":\"Pão\",\"amount\":\"4.00\",\"currency\":\"BRL\",\"date\":\"2026-10-25\","
                + "\"categoryId\":\"" + food + "\",\"subcategoryId\":\"" + bakery + "\"}"), token);
        assertEquals(201, withSub.getResponse().getStatus());

        MvcResult result = overview(token, "2026-10", "BRL");

        List<String> dates = JsonPath.read(text(result), "$.latestTransactions[*].date");
        assertEquals(8, dates.size());
        assertEquals("2026-10-25", dates.get(0));
        assertEquals("2026-10-12", dates.get(1));
        assertEquals("2026-10-06", dates.get(7));
        assertEquals("Pão", field(result, "$.latestTransactions[0].description"));
        assertEquals("Padaria", field(result, "$.latestTransactions[0].subcategory.name"));
        assertEquals("Alimentação", field(result, "$.latestTransactions[0].category.name"));
        assertFalse(text(result).contains("\"3.00\""), "EUR não entra em BRL");
    }

    @Test
    void budgetProgressFollowsTheEffectiveLimitOfTheMonth() throws Exception {
        String token = newUserToken();
        String food = categoryId(token, "Alimentação");
        add(token, "EXPENSE", "850.00", "2026-10-02", food, "BRL");
        assertEquals("NONE", field(overview(token, "2026-10", "BRL"), "$.budgetProgress.status"));

        call(put("/api/v1/budgets/BRL").contentType(MediaType.APPLICATION_JSON)
            .content("{\"month\":\"2026-10\",\"monthlyLimit\":\"1000\",\"categoryLimits\":[]}"), token);

        MvcResult result = overview(token, "2026-10", "BRL");
        assertEquals("WARNING", field(result, "$.budgetProgress.status"));
        assertEquals("1000.00", field(result, "$.budgetProgress.limit"));
        assertEquals("850.00", field(result, "$.budgetProgress.spent"));
        assertEquals("0.8500", field(result, "$.budgetProgress.fraction"));
        assertEquals("150.00", field(result, "$.budgetProgress.remaining"));

        // ajuste do mês sobrepõe o padrão; outro mês e outra moeda não têm gasto nem meta
        call(put("/api/v1/budgets/BRL").contentType(MediaType.APPLICATION_JSON)
            .content("{\"month\":\"2026-10\",\"monthlyLimit\":\"1000\",\"monthOverride\":{\"limit\":\"800\"},\"categoryLimits\":[]}"), token);
        assertEquals("EXCEEDED", field(overview(token, "2026-10", "BRL"), "$.budgetProgress.status"));
        assertEquals("OK", field(overview(token, "2026-11", "BRL"), "$.budgetProgress.status"));
        assertEquals("NONE", field(overview(token, "2026-10", "EUR"), "$.budgetProgress.status"));

        call(put("/api/v1/budgets/BRL").contentType(MediaType.APPLICATION_JSON)
            .content("{\"month\":\"2026-10\",\"monthlyLimit\":\"1000\",\"monthOverride\":{\"disabled\":true},\"categoryLimits\":[]}"), token);
        MvcResult disabled = overview(token, "2026-10", "BRL");
        assertEquals("NONE", field(disabled, "$.budgetProgress.status"));
        assertFalse(text(disabled).contains("\"limit\""));
    }

    @Test
    void usersNeverSeeEachOthersData() throws Exception {
        String ana = newUserToken();
        String rui = newUserToken();
        add(ana, "INCOME", "1000.00", "2026-10-02", categoryId(ana, "Salário"), "BRL");

        MvcResult ruiView = overview(rui, "2026-10", "BRL");

        assertEquals("0.00", field(ruiView, "$.summary.income"));
        assertEquals(0, ((List<?>) JsonPath.read(text(ruiView), "$.latestTransactions")).size());
        assertEquals("1000.00", field(overview(ana, "2026-10", "BRL"), "$.summary.income"));
    }

    @Test
    void aCallRunsAHandfulOfAggregatedStatements() throws Exception {
        String token = newUserToken();
        String food = categoryId(token, "Alimentação");
        for (int i = 0; i < 40; i++) {
            add(token, "EXPENSE", "1.00", "2026-10-" + String.format("%02d", 1 + i % 28), food, "BRL");
        }
        overview(token, "2026-10", "BRL"); // aquece
        var statistics = entityManagerFactory.unwrap(SessionFactory.class).getStatistics();

        long before = statistics.getPrepareStatementCount();
        MvcResult result = overview(token, "2026-10", "BRL");
        long statements = statistics.getPrepareStatementCount() - before;

        assertEquals(200, result.getResponse().getStatus());
        // 40 lançamentos e 8 itens: o número de comandos não cresce com os dados (sem N+1)
        assertTrue(statements <= 14, "comandos SQL por chamada: " + statements);
    }
}
