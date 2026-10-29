package com.dowglasmaia.financecontroller.transaction;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;

import com.dowglasmaia.financecontroller.support.AbstractPostgresIntegrationTest;
import com.jayway.jsonpath.JsonPath;
import java.util.List;
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
 * Transactions through the real HTTP stack and a real PostgreSQL (Testcontainers): validation, category
 * compatibility, monthly listing with totals and pagination, edit/delete, isolation between users and currencies,
 * and the category rules that depend on transactions (in use, type conflict, subcategory removal).
 */
@AutoConfigureMockMvc
class TransactionFlowIntegrationTest extends AbstractPostgresIntegrationTest {
    private static final String PASSWORD = "Sup3r-Secret-Passw0rd";

    @DynamicPropertySource
    static void identityProperties(DynamicPropertyRegistry registry) {
        registry.add("app.identity.jwt.secret", () -> "test-only-dummy-jwt-secret-0123456789-abcdef");
    }

    @Autowired
    private MockMvc mvc;

    private String newUserToken() throws Exception {
        String email = "txn." + UUID.randomUUID() + "@example.com";
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

    private static MockHttpServletRequestBuilder json(MockHttpServletRequestBuilder request, String body) {
        return request.contentType(MediaType.APPLICATION_JSON).content(body);
    }

    private static String text(MvcResult r) throws Exception {
        return r.getResponse().getContentAsString();
    }

    private static String code(MvcResult r) throws Exception {
        return JsonPath.read(text(r), "$.code");
    }

    /** Id of a seeded category of the user, by name. */
    private String categoryId(String token, String name) throws Exception {
        List<String> ids = JsonPath.read(text(call(get("/api/v1/categories"), token)), "$[?(@.name=='" + name + "')].id");
        return ids.get(0);
    }

    private String subcategoryId(String token, String category, String name) throws Exception {
        List<String> ids = JsonPath.read(text(call(get("/api/v1/categories"), token)),
            "$[?(@.name=='" + category + "')].subcategories[?(@.name=='" + name + "')].id");
        return ids.get(0);
    }

    private static String body(String kind, String amount, String currency, String date, String categoryId,
                               String subcategoryId) {
        return "{\"kind\":\"" + kind + "\",\"description\":\"Compra\",\"amount\":\"" + amount + "\",\"currency\":\""
            + currency + "\",\"date\":\"" + date + "\",\"categoryId\":\"" + categoryId + "\""
            + (subcategoryId == null ? "" : ",\"subcategoryId\":\"" + subcategoryId + "\"") + "}";
    }

    private MvcResult create(String token, String json) throws Exception {
        return call(json(post("/api/v1/transactions"), json), token);
    }

    private MvcResult list(String token, String query) throws Exception {
        return call(get("/api/v1/transactions" + query), token);
    }

    @Test
    void requiresAuthentication() throws Exception {
        assertEquals(401, mvc.perform(get("/api/v1/transactions?month=2026-10&currency=BRL")).andReturn()
            .getResponse().getStatus());
    }

    @Test
    void createReturnsTheTransactionWithCategoryAndSubcategoryAndExactDecimal() throws Exception {
        String token = newUserToken();
        String food = categoryId(token, "Alimentação");
        String market = subcategoryId(token, "Alimentação", "Supermercado");

        MvcResult created = create(token, body("EXPENSE", "1234.5", "BRL", "2026-10-05", food, market));

        assertEquals(201, created.getResponse().getStatus());
        assertEquals("1234.50", JsonPath.read(text(created), "$.transaction.amount"));
        assertEquals("Alimentação", JsonPath.read(text(created), "$.transaction.category.name"));
        assertEquals("Supermercado", JsonPath.read(text(created), "$.transaction.subcategory.name"));
        assertEquals("BRL", JsonPath.read(text(created), "$.transaction.currency"));
        // sem metas (F6) nunca há alerta
        assertFalse(text(created).contains("budgetAlert"));
    }

    @Test
    void validationErrorsAre400WithFieldMessages() throws Exception {
        String token = newUserToken();
        String food = categoryId(token, "Alimentação");

        MvcResult zero = create(token, body("EXPENSE", "0", "BRL", "2026-10-05", food, null));
        assertEquals(400, zero.getResponse().getStatus());
        assertEquals("VALIDATION_FAILED", code(zero));
        assertEquals("amount", JsonPath.read(text(zero), "$.errors[0].field"));
        assertEquals("O valor deve ser maior que zero", JsonPath.read(text(zero), "$.errors[0].message"));

        for (String amount : new String[] {"-1", "1.234,56", "abc", "10000000000.00"}) {
            assertEquals(400, create(token, body("EXPENSE", amount, "BRL", "2026-10-05", food, null)).getResponse().getStatus(), amount);
        }
        assertEquals(400, create(token, body("EXPENSE", "10.00", "USD", "2026-10-05", food, null)).getResponse().getStatus());
        assertEquals(400, create(token, body("EXPENSE", "10.00", "BRL", "1999-12-31", food, null)).getResponse().getStatus());
        assertEquals(400, create(token, body("EXPENSE", "10.00", "BRL", "2101-01-01", food, null)).getResponse().getStatus());
        assertEquals(400, create(token, body("EXPENSE", "10.00", "BRL", "2026-10-05", food, null)
            .replace("\"Compra\"", "\"   \"")).getResponse().getStatus());
    }

    @Test
    void futureDatesAreAccepted() throws Exception {
        String token = newUserToken();
        String food = categoryId(token, "Alimentação");

        assertEquals(201, create(token, body("EXPENSE", "10.00", "BRL", "2099-06-01", food, null)).getResponse().getStatus());
    }

    @Test
    void categoryMustAcceptTheKindAndSubcategoryMustBelongToIt() throws Exception {
        String token = newUserToken();
        String food = categoryId(token, "Alimentação");
        String salary = categoryId(token, "Salário");
        String clt = subcategoryId(token, "Salário", "CLT");

        MvcResult mismatch = create(token, body("INCOME", "10.00", "BRL", "2026-10-05", food, null));
        assertEquals(409, mismatch.getResponse().getStatus());
        assertEquals("CATEGORY_TYPE_MISMATCH", code(mismatch));

        MvcResult foreignSub = create(token, body("EXPENSE", "10.00", "BRL", "2026-10-05", food, clt));
        assertEquals(409, foreignSub.getResponse().getStatus());
        assertEquals("SUBCATEGORY_NOT_IN_CATEGORY", code(foreignSub));

        assertEquals(201, create(token, body("INCOME", "10.00", "BRL", "2026-10-05", salary, clt)).getResponse().getStatus());

        // categoria BOTH aceita as duas direções
        MvcResult both = call(json(post("/api/v1/categories"),
            "{\"name\":\"Extras\",\"type\":\"BOTH\",\"icon\":\"sell\",\"color\":\"#112233\"}"), token);
        String bothId = JsonPath.read(text(both), "$.id");
        assertEquals(201, create(token, body("INCOME", "1.00", "BRL", "2026-10-05", bothId, null)).getResponse().getStatus());
        assertEquals(201, create(token, body("EXPENSE", "1.00", "BRL", "2026-10-05", bothId, null)).getResponse().getStatus());
    }

    @Test
    void listIsScopedToMonthAndCurrencyOrderedWithTotalsOfTheWholeSetAndPaginated() throws Exception {
        String token = newUserToken();
        String food = categoryId(token, "Alimentação");
        String salary = categoryId(token, "Salário");
        for (int day = 1; day <= 17; day++) {
            create(token, body("EXPENSE", "10.00", "BRL", "2026-10-" + String.format("%02d", day), food, null));
        }
        create(token, body("INCOME", "500.00", "BRL", "2026-10-02", salary, null));
        create(token, body("EXPENSE", "10.00", "BRL", "2026-09-30", food, null));
        create(token, body("EXPENSE", "77.00", "EUR", "2026-10-05", food, null));

        MvcResult first = list(token, "?month=2026-10&currency=BRL");
        assertEquals(200, first.getResponse().getStatus());
        assertEquals(15, ((List<?>) JsonPath.read(text(first), "$.items")).size());
        assertEquals(18, ((Number) JsonPath.read(text(first), "$.totalItems")).intValue());
        assertEquals(2, (int) JsonPath.read(text(first), "$.totalPages"));
        assertEquals("2026-10-17", JsonPath.read(text(first), "$.items[0].date"));
        // totais do conjunto filtrado inteiro (não só da página), sem EUR nem setembro
        assertEquals("500.00", JsonPath.read(text(first), "$.totals.income"));
        assertEquals("170.00", JsonPath.read(text(first), "$.totals.expense"));
        assertEquals("330.00", JsonPath.read(text(first), "$.totals.balance"));

        MvcResult second = list(token, "?month=2026-10&currency=BRL&page=2");
        assertEquals(3, ((List<?>) JsonPath.read(text(second), "$.items")).size());

        MvcResult incomeOnly = list(token, "?month=2026-10&currency=BRL&type=INCOME");
        assertEquals(1, ((Number) JsonPath.read(text(incomeOnly), "$.totalItems")).intValue());
        assertEquals("0.00", JsonPath.read(text(incomeOnly), "$.totals.expense"));

        MvcResult eur = list(token, "?month=2026-10&currency=EUR");
        assertEquals("77.00", JsonPath.read(text(eur), "$.totals.expense"));
        assertEquals("-77.00", JsonPath.read(text(eur), "$.totals.balance"));

        MvcResult empty = list(token, "?month=2030-01&currency=BRL");
        assertEquals(1, (int) JsonPath.read(text(empty), "$.totalPages"));
        assertEquals("0.00", JsonPath.read(text(empty), "$.totals.balance"));
    }

    @Test
    void listRejectsInvalidParameters() throws Exception {
        String token = newUserToken();

        assertEquals(400, list(token, "?month=2026-13&currency=BRL").getResponse().getStatus());
        assertEquals(400, list(token, "?month=2026-10").getResponse().getStatus());
        assertEquals(400, list(token, "?month=2026-10&currency=BRL&type=ALLL").getResponse().getStatus());
        assertEquals(400, list(token, "?month=2026-10&currency=BRL&page=0").getResponse().getStatus());
        assertEquals(400, list(token, "?month=2026-10&currency=BRL&size=101").getResponse().getStatus());
    }

    @Test
    void sameDateTransactionsKeepTheMostRecentlyCreatedFirst() throws Exception {
        String token = newUserToken();
        String food = categoryId(token, "Alimentação");
        String firstId = JsonPath.read(text(create(token, body("EXPENSE", "1.00", "BRL", "2026-10-05", food, null))), "$.transaction.id");
        String secondId = JsonPath.read(text(create(token, body("EXPENSE", "2.00", "BRL", "2026-10-05", food, null))), "$.transaction.id");

        List<String> ids = JsonPath.read(text(list(token, "?month=2026-10&currency=BRL")), "$.items[*].id");

        assertEquals(List.of(secondId, firstId), ids);
    }

    @Test
    void editPreservesIdAndCreatedAtAndUpdatesTheRest() throws Exception {
        String token = newUserToken();
        String food = categoryId(token, "Alimentação");
        String leisure = categoryId(token, "Lazer");
        MvcResult created = create(token, body("EXPENSE", "10.00", "BRL", "2026-10-05", food, null));
        String id = JsonPath.read(text(created), "$.transaction.id");
        String createdAt = JsonPath.read(text(created), "$.transaction.createdAt");
        Thread.sleep(15);

        MvcResult edited = call(json(put("/api/v1/transactions/" + id),
            body("EXPENSE", "99.90", "EUR", "2026-11-02", leisure, null)), token);

        assertEquals(200, edited.getResponse().getStatus());
        assertEquals(id, JsonPath.read(text(edited), "$.transaction.id"));
        assertEquals(createdAt, JsonPath.read(text(edited), "$.transaction.createdAt"));
        assertNotEquals(createdAt, JsonPath.read(text(edited), "$.transaction.updatedAt"));
        assertEquals("99.90", JsonPath.read(text(edited), "$.transaction.amount"));
        assertEquals("Lazer", JsonPath.read(text(edited), "$.transaction.category.name"));
        // saiu de BRL/outubro e entrou em EUR/novembro
        assertEquals(0, ((Number) JsonPath.read(text(list(token, "?month=2026-10&currency=BRL")), "$.totalItems")).intValue());
        assertEquals(1, ((Number) JsonPath.read(text(list(token, "?month=2026-11&currency=EUR")), "$.totalItems")).intValue());

        MvcResult fetched = call(get("/api/v1/transactions/" + id), token);
        assertEquals("99.90", JsonPath.read(text(fetched), "$.amount"));
    }

    @Test
    void deleteRemovesAndASecondDeleteIs404() throws Exception {
        String token = newUserToken();
        String food = categoryId(token, "Alimentação");
        String id = JsonPath.read(text(create(token, body("EXPENSE", "10.00", "BRL", "2026-10-05", food, null))), "$.transaction.id");

        assertEquals(204, call(delete("/api/v1/transactions/" + id), token).getResponse().getStatus());
        assertEquals(404, call(delete("/api/v1/transactions/" + id), token).getResponse().getStatus());
        assertEquals(404, call(get("/api/v1/transactions/" + id), token).getResponse().getStatus());
    }

    @Test
    void usersNeverSeeOrChangeEachOthersTransactionsOrUseEachOthersCategories() throws Exception {
        String ana = newUserToken();
        String rui = newUserToken();
        String anaFood = categoryId(ana, "Alimentação");
        String ruiFood = categoryId(rui, "Alimentação");
        String anaTxn = JsonPath.read(text(create(ana, body("EXPENSE", "10.00", "BRL", "2026-10-05", anaFood, null))), "$.transaction.id");

        assertEquals(404, call(get("/api/v1/transactions/" + anaTxn), rui).getResponse().getStatus());
        assertEquals(404, call(delete("/api/v1/transactions/" + anaTxn), rui).getResponse().getStatus());
        assertEquals(404, call(json(put("/api/v1/transactions/" + anaTxn),
            body("EXPENSE", "1.00", "BRL", "2026-10-05", ruiFood, null)), rui).getResponse().getStatus());
        assertEquals(0, ((Number) JsonPath.read(text(list(rui, "?month=2026-10&currency=BRL")), "$.totalItems")).intValue());
        // categoria de outro utilizador → 404, como se não existisse
        assertEquals(404, create(rui, body("EXPENSE", "10.00", "BRL", "2026-10-05", anaFood, null)).getResponse().getStatus());
        // nada de Ana mudou
        assertEquals("10.00", JsonPath.read(text(call(get("/api/v1/transactions/" + anaTxn), ana)), "$.amount"));
    }

    @Test
    void categoryWithTransactionsCannotBeDeletedAndTypeChangeIsRefusedWhenIncompatible() throws Exception {
        String token = newUserToken();
        String food = categoryId(token, "Alimentação");
        String txn = JsonPath.read(text(create(token, body("EXPENSE", "10.00", "BRL", "2026-10-05", food, null))), "$.transaction.id");

        MvcResult inUse = call(delete("/api/v1/categories/" + food), token);
        assertEquals(409, inUse.getResponse().getStatus());
        assertEquals("CATEGORY_IN_USE", code(inUse));
        assertEquals(200, call(get("/api/v1/categories/" + food), token).getResponse().getStatus());

        String asIncome = "{\"name\":\"Alimentação\",\"type\":\"INCOME\",\"icon\":\"restaurant\",\"color\":\"#4CAF50\"}";
        MvcResult conflict = call(json(put("/api/v1/categories/" + food), asIncome), token);
        assertEquals(409, conflict.getResponse().getStatus());
        assertEquals("CATEGORY_TYPE_CONFLICT", code(conflict));
        assertEquals("EXPENSE", JsonPath.read(text(call(get("/api/v1/categories/" + food), token)), "$.type"));

        // BOTH continua compatível com o lançamento existente
        String asBoth = "{\"name\":\"Alimentação\",\"type\":\"BOTH\",\"icon\":\"restaurant\",\"color\":\"#4CAF50\"}";
        assertEquals(200, call(json(put("/api/v1/categories/" + food), asBoth), token).getResponse().getStatus());

        // sem lançamentos, a categoria pode ser excluída
        assertEquals(204, call(delete("/api/v1/transactions/" + txn), token).getResponse().getStatus());
        assertEquals(204, call(delete("/api/v1/categories/" + food), token).getResponse().getStatus());
    }

    @Test
    void deletingASubcategoryKeepsTheTransactionWithoutIt() throws Exception {
        String token = newUserToken();
        String food = categoryId(token, "Alimentação");
        String bakery = subcategoryId(token, "Alimentação", "Padaria");
        String txn = JsonPath.read(text(create(token, body("EXPENSE", "10.00", "BRL", "2026-10-05", food, bakery))), "$.transaction.id");

        assertEquals(204, call(delete("/api/v1/categories/" + food + "/subcategories/" + bakery), token).getResponse().getStatus());

        MvcResult fetched = call(get("/api/v1/transactions/" + txn), token);
        assertEquals(200, fetched.getResponse().getStatus());
        assertFalse(text(fetched).contains("\"subcategory\""));
        assertEquals("Alimentação", JsonPath.read(text(fetched), "$.category.name"));
        assertTrue(text(fetched).contains("\"amount\":\"10.00\""));
    }
}
