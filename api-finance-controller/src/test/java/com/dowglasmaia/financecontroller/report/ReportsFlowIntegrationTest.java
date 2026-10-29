package com.dowglasmaia.financecontroller.report;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

import com.dowglasmaia.financecontroller.support.AbstractPostgresIntegrationTest;
import com.jayway.jsonpath.JsonPath;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
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
 * The period report through the real HTTP stack and a real PostgreSQL (Testcontainers): week/month/year ranges, bars
 * that match the totals, expense distribution and ranking, yearly summary, isolation between users and currencies.
 */
@AutoConfigureMockMvc
class ReportsFlowIntegrationTest extends AbstractPostgresIntegrationTest {
    private static final String PASSWORD = "Sup3r-Secret-Passw0rd";

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
        registry.add("app.identity.jwt.secret", () -> "test-only-dummy-jwt-secret-0123456789-abcdef");
    }

    @Autowired
    private MockMvc mvc;

    private String newUserToken() throws Exception {
        String email = "rep." + UUID.randomUUID() + "@example.com";
        MvcResult result = mvc.perform(post("/api/v1/auth/register").with(r -> {
            r.setRemoteAddr("10.9." + (int) (Math.random() * 250) + "." + (int) (Math.random() * 250));
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

    private String newExpenseCategory(String token, String name) throws Exception {
        MvcResult r = call(post("/api/v1/categories").contentType(MediaType.APPLICATION_JSON)
            .content("{\"name\":\"" + name + "\",\"type\":\"EXPENSE\",\"icon\":\"sell\",\"color\":\"#FF9800\"}"), token);
        assertEquals(201, r.getResponse().getStatus(), text(r));
        return JsonPath.read(text(r), "$.id");
    }

    private void add(String token, String kind, String amount, String date, String categoryId, String currency) throws Exception {
        MvcResult r = call(post("/api/v1/transactions").contentType(MediaType.APPLICATION_JSON)
            .content("{\"kind\":\"" + kind + "\",\"description\":\"" + kind.toLowerCase() + " " + amount + "\",\"amount\":\"" + amount
                + "\",\"currency\":\"" + currency + "\",\"date\":\"" + date + "\",\"categoryId\":\"" + categoryId + "\"}"), token);
        assertEquals(201, r.getResponse().getStatus(), text(r));
    }

    private MvcResult report(String token, String period, String anchor, String currency) throws Exception {
        return call(get("/api/v1/reports?period=" + period + "&anchorDate=" + anchor + "&currency=" + currency), token);
    }

    private static String field(MvcResult r, String path) throws Exception {
        return JsonPath.read(text(r), path);
    }

    private static BigDecimal sum(MvcResult r, String path) throws Exception {
        List<String> values = JsonPath.read(text(r), path);
        return values.stream().map(BigDecimal::new).reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    @Test
    void requiresAuthentication() throws Exception {
        assertEquals(401, mvc.perform(get("/api/v1/reports?period=MONTH&anchorDate=2026-06-15&currency=BRL")).andReturn().getResponse().getStatus());
    }

    @Test
    void invalidParametersAre400() throws Exception {
        String token = newUserToken();

        for (String query : List.of(
            "period=DAY&anchorDate=2026-06-15&currency=BRL",
            "period=MONTH&anchorDate=2026-13-45&currency=BRL",
            "period=MONTH&anchorDate=2026-06-15&currency=USD",
            "anchorDate=2026-06-15&currency=BRL",
            "period=MONTH&currency=BRL",
            "period=MONTH&anchorDate=2026-06-15",
            "period=MONTH&anchorDate=1999-12-31&currency=BRL",
            "period=MONTH&anchorDate=2101-01-01&currency=BRL",
            "period=MONTH&anchorDate=2026-06-15&currency=BRL&rankingSize=0",
            "period=MONTH&anchorDate=2026-06-15&currency=BRL&rankingSize=51")) {
            assertEquals(400, call(get("/api/v1/reports?" + query), token).getResponse().getStatus(), query);
        }
    }

    @Test
    void weekIsSundayToSaturdayWithSevenBarsAndInclusiveEdges() throws Exception {
        String token = newUserToken();
        String salary = categoryId(token, "Salário");
        add(token, "INCOME", "100.00", "2026-06-20", salary, "BRL");
        add(token, "INCOME", "10.00", "2026-06-21", salary, "BRL");
        add(token, "INCOME", "5.50", "2026-06-24", salary, "BRL");
        add(token, "INCOME", "1.25", "2026-06-27", salary, "BRL");
        add(token, "INCOME", "200.00", "2026-06-28", salary, "BRL");

        MvcResult result = report(token, "WEEK", "2026-06-24", "BRL");

        assertEquals(200, result.getResponse().getStatus(), text(result));
        assertEquals("2026-06-21", field(result, "$.range.start"));
        assertEquals("2026-06-27", field(result, "$.range.end"));
        assertEquals("16.75", field(result, "$.summary.income"));
        assertEquals(7, ((List<?>) JsonPath.read(text(result), "$.bars")).size());
        assertEquals("Dom", field(result, "$.bars[0].label"));
        assertEquals("Sáb", field(result, "$.bars[6].label"));
        assertEquals("10.00", field(result, "$.bars[0].income"));
        assertEquals("1.25", field(result, "$.bars[6].income"));
        assertEquals(new BigDecimal("16.75"), sum(result, "$.bars[*].income"));
        assertTrue(((Map<?, ?>) JsonPath.read(text(result), "$")).get("monthlySummary") == null);
    }

    @Test
    void weekAcrossTheTurnOfYearIsOneContinuousRange() throws Exception {
        String token = newUserToken();
        String salary = categoryId(token, "Salário");
        add(token, "INCOME", "30.00", "2026-12-28", salary, "BRL");
        add(token, "INCOME", "70.00", "2027-01-01", salary, "BRL");
        add(token, "INCOME", "999.00", "2027-01-03", salary, "BRL");

        MvcResult result = report(token, "WEEK", "2026-12-31", "BRL");

        assertEquals("2026-12-27", field(result, "$.range.start"));
        assertEquals("2027-01-02", field(result, "$.range.end"));
        assertEquals("100.00", field(result, "$.summary.income"));
        assertEquals("30.00", field(result, "$.bars[1].income"));
        assertEquals("70.00", field(result, "$.bars[5].income"));
    }

    @Test
    void monthBarsAddUpToTheTotalsForAnyReferenceMonthAndLengthOfMonth() throws Exception {
        String token = newUserToken();
        String salary = categoryId(token, "Salário");
        String food = categoryId(token, "Alimentação");
        add(token, "INCOME", "1000.00", "2026-06-01", salary, "BRL");
        add(token, "INCOME", "250.50", "2026-06-30", salary, "BRL");
        add(token, "EXPENSE", "75.25", "2026-06-15", food, "BRL");
        add(token, "INCOME", "1.00", "2026-05-31", salary, "BRL");
        add(token, "INCOME", "1.00", "2026-07-01", salary, "BRL");
        add(token, "INCOME", "40.00", "2026-02-28", salary, "BRL");
        add(token, "INCOME", "60.00", "2028-02-29", salary, "BRL");

        MvcResult june = report(token, "MONTH", "2026-06-15", "BRL");
        assertEquals("2026-06-01", field(june, "$.range.start"));
        assertEquals("2026-06-30", field(june, "$.range.end"));
        assertEquals(5, ((List<?>) JsonPath.read(text(june), "$.bars")).size());
        assertEquals("1250.50", field(june, "$.summary.income"));
        assertEquals("75.25", field(june, "$.summary.expense"));
        assertEquals("1175.25", field(june, "$.summary.balance"));
        assertEquals(new BigDecimal("1250.50"), sum(june, "$.bars[*].income"));
        assertEquals(new BigDecimal("75.25"), sum(june, "$.bars[*].expense"));
        assertEquals("S1", field(june, "$.bars[0].label"));
        assertEquals("250.50", field(june, "$.bars[4].income"));

        MvcResult february = report(token, "MONTH", "2026-02-15", "BRL");
        assertEquals(4, ((List<?>) JsonPath.read(text(february), "$.bars")).size());
        assertEquals("40.00", field(february, "$.bars[3].income"));

        MvcResult leapFebruary = report(token, "MONTH", "2028-02-15", "BRL");
        assertEquals(5, ((List<?>) JsonPath.read(text(leapFebruary), "$.bars")).size());
        assertEquals("60.00", field(leapFebruary, "$.bars[4].income"));
        assertEquals("2028-02-29", field(leapFebruary, "$.range.end"));
    }

    @Test
    void yearHasTwelveBarsAndAMonthlySummaryWithBalanceChange() throws Exception {
        String token = newUserToken();
        String salary = categoryId(token, "Salário");
        String food = categoryId(token, "Alimentação");
        add(token, "INCOME", "100.00", "2026-01-10", salary, "BRL");
        add(token, "INCOME", "150.00", "2026-02-10", salary, "BRL");
        // março vazio (saldo 0); abril, de novo vazio, não tem base
        add(token, "EXPENSE", "50.00", "2026-05-10", food, "BRL");
        add(token, "INCOME", "50.00", "2026-06-10", salary, "BRL");
        add(token, "INCOME", "999.00", "2025-12-31", salary, "BRL");
        add(token, "INCOME", "999.00", "2027-01-01", salary, "BRL");

        MvcResult result = report(token, "YEAR", "2026-06-15", "BRL");

        assertEquals(200, result.getResponse().getStatus(), text(result));
        assertEquals("2026-01-01", field(result, "$.range.start"));
        assertEquals("2026-12-31", field(result, "$.range.end"));
        assertEquals("300.00", field(result, "$.summary.income"));
        assertEquals("50.00", field(result, "$.summary.expense"));
        assertEquals(12, ((List<?>) JsonPath.read(text(result), "$.bars")).size());
        assertEquals("Jan", field(result, "$.bars[0].label"));
        assertEquals("Dez", field(result, "$.bars[11].label"));
        assertEquals(new BigDecimal("300.00"), sum(result, "$.bars[*].income"));

        List<Map<String, Object>> months = JsonPath.read(text(result), "$.monthlySummary");
        assertEquals(12, months.size());
        assertEquals("2026-01", months.get(0).get("month"));
        assertFalse(months.get(0).containsKey("changePercent"), "janeiro não tem mês anterior");
        assertEquals("50.0", months.get(1).get("changePercent"));
        assertEquals("-100.0", months.get(2).get("changePercent"));
        assertFalse(months.get(3).containsKey("changePercent"), "saldo anterior 0");
        assertEquals("-50.00", months.get(4).get("balance"));
        assertFalse(months.get(4).containsKey("changePercent"), "saldo anterior (abril) 0");
        assertEquals("200.0", months.get(5).get("changePercent"));
        assertEquals("100.00", months.get(0).get("income"));
    }

    @Test
    void distributionAndRankingCountOnlyExpensesOfTheCurrencyAndRankingIsLimited() throws Exception {
        String token = newUserToken();
        String salary = categoryId(token, "Salário");
        List<String> names = new ArrayList<>(List.of("Alimentação", "Carro", "Moradia", "Lazer", "Saúde", "Educação", "Vestuário"));
        List<String> ids = new ArrayList<>();
        for (String name : names) {
            ids.add(categoryId(token, name));
        }
        for (String extra : List.of("Extra A", "Extra B", "Extra C")) {
            names.add(extra);
            ids.add(newExpenseCategory(token, extra));
        }
        // 10 categorias com 100, 90, … , 10
        for (int i = 0; i < ids.size(); i++) {
            add(token, "EXPENSE", (100 - i * 10) + ".00", "2026-06-10", ids.get(i), "BRL");
        }
        add(token, "INCOME", "5000.00", "2026-06-10", salary, "BRL");
        add(token, "EXPENSE", "777.00", "2026-06-10", ids.get(9), "EUR");

        MvcResult result = report(token, "MONTH", "2026-06-15", "BRL");

        assertEquals("550.00", field(result, "$.summary.expense"));
        assertEquals(10, ((List<?>) JsonPath.read(text(result), "$.expenseByCategory")).size());
        assertEquals(7, ((List<?>) JsonPath.read(text(result), "$.ranking")).size());
        assertEquals(names.get(0), field(result, "$.ranking[0].category.name"));
        assertEquals("100.00", field(result, "$.ranking[0].amount"));
        assertEquals("18.1818", field(result, "$.ranking[0].percentage"));
        assertEquals("40.00", field(result, "$.ranking[6].amount"));
        BigDecimal percentages = sum(result, "$.expenseByCategory[*].percentage");
        assertTrue(percentages.subtract(new BigDecimal("100")).abs().compareTo(new BigDecimal("0.01")) < 0, percentages.toPlainString());

        MvcResult three = call(get("/api/v1/reports?period=MONTH&anchorDate=2026-06-15&currency=BRL&rankingSize=3"), token);
        assertEquals(3, ((List<?>) JsonPath.read(text(three), "$.ranking")).size());
        assertEquals(10, ((List<?>) JsonPath.read(text(three), "$.expenseByCategory")).size());
        // o percentual do ranking continua sobre o total de todas as despesas
        assertEquals("18.1818", field(three, "$.ranking[0].percentage"));

        MvcResult eur = report(token, "MONTH", "2026-06-15", "EUR");
        assertEquals("777.00", field(eur, "$.summary.expense"));
        assertEquals(1, ((List<?>) JsonPath.read(text(eur), "$.ranking")).size());
        assertEquals("100.0000", field(eur, "$.ranking[0].percentage"));
    }

    @Test
    void emptyPeriodIsZerosAndEmptyListsNotAnError() throws Exception {
        String token = newUserToken();

        MvcResult result = report(token, "MONTH", "2030-05-15", "BRL");

        assertEquals(200, result.getResponse().getStatus());
        assertEquals("0.00", field(result, "$.summary.income"));
        assertEquals("0.00", field(result, "$.summary.balance"));
        assertEquals(5, ((List<?>) JsonPath.read(text(result), "$.bars")).size());
        assertEquals(new BigDecimal("0.00"), sum(result, "$.bars[*].expense"));
        assertEquals("[]", JsonPath.parse(text(result)).read("$.expenseByCategory").toString());
        assertEquals("[]", JsonPath.parse(text(result)).read("$.ranking").toString());

        MvcResult year = report(token, "YEAR", "2030-05-15", "BRL");
        assertEquals(12, ((List<?>) JsonPath.read(text(year), "$.monthlySummary")).size());
        assertNull(((Map<?, ?>) ((List<?>) JsonPath.read(text(year), "$.monthlySummary")).get(5)).get("changePercent"));
    }

    @Test
    void reportsOnlyCountTheAuthenticatedUsersData() throws Exception {
        String owner = newUserToken();
        String stranger = newUserToken();
        add(owner, "INCOME", "123.00", "2026-06-10", categoryId(owner, "Salário"), "BRL");
        add(owner, "EXPENSE", "45.00", "2026-06-11", categoryId(owner, "Alimentação"), "BRL");

        MvcResult mine = report(owner, "MONTH", "2026-06-15", "BRL");
        MvcResult theirs = report(stranger, "MONTH", "2026-06-15", "BRL");

        assertEquals("123.00", field(mine, "$.summary.income"));
        assertEquals("0.00", field(theirs, "$.summary.income"));
        assertEquals("0.00", field(theirs, "$.summary.expense"));
        assertEquals(0, ((List<?>) JsonPath.read(text(theirs), "$.ranking")).size());
    }

    @Test
    void moneyBoxMovementsAreNotPartOfTheReport() throws Exception {
        String token = newUserToken();
        MvcResult box = call(post("/api/v1/money-boxes").contentType(MediaType.APPLICATION_JSON)
            .content("{\"name\":\"Reserva\",\"icon\":\"savings\",\"color\":\"#22C55E\",\"currency\":\"BRL\"}"), token);
        String boxId = JsonPath.read(text(box), "$.id");
        call(post("/api/v1/money-boxes/" + boxId + "/entries").contentType(MediaType.APPLICATION_JSON)
            .content("{\"kind\":\"DEPOSIT\",\"amount\":\"500.00\",\"date\":\"2026-06-10\"}"), token);

        MvcResult result = report(token, "MONTH", "2026-06-15", "BRL");

        assertEquals("0.00", field(result, "$.summary.income"));
        assertEquals("0.00", field(result, "$.summary.expense"));
        assertEquals(0, ((List<?>) JsonPath.read(text(result), "$.expenseByCategory")).size());
    }
}
