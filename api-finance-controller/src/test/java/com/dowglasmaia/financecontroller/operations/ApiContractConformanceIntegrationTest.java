package com.dowglasmaia.financecontroller.operations;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;

import com.dowglasmaia.financecontroller.support.AbstractPostgresIntegrationTest;
import com.jayway.jsonpath.JsonPath;
import jakarta.servlet.http.Cookie;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.UUID;
import java.util.stream.Stream;
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
import org.yaml.snakeyaml.Yaml;
import tools.jackson.databind.DeserializationFeature;
import tools.jackson.databind.JavaType;
import tools.jackson.databind.json.JsonMapper;

/**
 * QUALITY-SPEC-03 without a third-party validator: the OpenAPI files themselves drive the checks. Every operation is
 * called for real; the status must be one the contract declares, a success body must deserialize into the generated model
 * (unknown fields fail) and satisfy its constraints, and an error body must match the {@code ProblemDetail} schema read
 * from {@code openapi/common/responses.yaml}. A final test fails if any {@code operationId} of any contract was not exercised.
 */
@AutoConfigureMockMvc
class ApiContractConformanceIntegrationTest extends AbstractPostgresIntegrationTest {
    private static final Path MODULES = Path.of("src", "main", "resources", "openapi", "modules");
    private static final Path COMMON = Path.of("src", "main", "resources", "openapi", "common", "responses.yaml");
    private static final JsonMapper STRICT = JsonMapper.builder().enable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES).build();
    private static final Validator VALIDATOR = Validation.buildDefaultValidatorFactory().getValidator();
    private static final String PASSWORD = "Sup3r-Secret-Passw0rd";

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
        registry.add("app.identity.jwt.secret", () -> "test-only-dummy-jwt-secret-0123456789-abcdef");
    }

    @Autowired
    private MockMvc mvc;

    private final Set<String> exercised = new TreeSet<>();

    // ------------------------------------------------------------------ o contrato como fonte

    @SuppressWarnings("unchecked")
    private Map<String, Set<String>> declaredStatuses() throws IOException {
        Map<String, Set<String>> byOperation = new java.util.HashMap<>();
        try (Stream<Path> files = Files.list(MODULES)) {
            for (Path file : files.filter(p -> p.toString().endsWith(".yaml")).toList()) {
                Map<String, Object> doc = new Yaml().load(Files.readString(file));
                Map<String, Map<String, Object>> paths = (Map<String, Map<String, Object>>) doc.get("paths");
                for (var path : paths.values()) {
                    for (var operation : path.values()) {
                        var op = (Map<String, Object>) operation;
                        var responses = (Map<Object, Object>) op.get("responses");
                        Set<String> statuses = new HashSet<>();
                        responses.keySet().forEach(k -> statuses.add(String.valueOf(k)));
                        byOperation.put((String) op.get("operationId"), statuses);
                    }
                }
            }
        }
        return byOperation;
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> problemSchema() throws IOException {
        Map<String, Object> doc = new Yaml().load(Files.readString(COMMON));
        Map<String, Object> components = (Map<String, Object>) doc.get("components");
        return (Map<String, Object>) ((Map<String, Object>) components.get("schemas")).get("ProblemDetail");
    }

    // ------------------------------------------------------------------ verificações

    /** Success: declared status, strict deserialization into the generated model, bean-validation constraints. */
    private <T> T ok(String operationId, int status, MvcResult result, Class<T> model) throws Exception {
        exercised.add(operationId);
        assertEquals(status, result.getResponse().getStatus(), operationId + ": " + result.getResponse().getContentAsString());
        assertTrue(declaredStatuses().get(operationId).contains(String.valueOf(status)), operationId + " não declara " + status);
        if (model == null) {
            assertEquals("", result.getResponse().getContentAsString(), operationId + " sem corpo");
            return null;
        }
        T value = STRICT.readValue(result.getResponse().getContentAsString(), model);
        assertTrue(VALIDATOR.validate(value).isEmpty(), operationId + ": " + VALIDATOR.validate(value));
        return value;
    }

    private <T> List<T> okList(String operationId, MvcResult result, Class<T> element) throws Exception {
        exercised.add(operationId);
        assertEquals(200, result.getResponse().getStatus(), operationId);
        assertTrue(declaredStatuses().get(operationId).contains("200"));
        JavaType type = STRICT.getTypeFactory().constructCollectionType(List.class, element);
        List<T> values = STRICT.readValue(result.getResponse().getContentAsString(), type);
        values.forEach(v -> assertTrue(VALIDATOR.validate(v).isEmpty(), operationId + ": " + VALIDATOR.validate(v)));
        return values;
    }

    /** Error: the status is declared (except the global 401) and the body matches the common ProblemDetail schema. */
    @SuppressWarnings("unchecked")
    private void problem(String operationId, int status, MvcResult result) throws Exception {
        exercised.add(operationId);
        assertEquals(status, result.getResponse().getStatus(), operationId + ": " + result.getResponse().getContentAsString());
        if (status != 401) {
            assertTrue(declaredStatuses().get(operationId).contains(String.valueOf(status)), operationId + " não declara " + status);
        }
        assertTrue(result.getResponse().getContentType().startsWith("application/problem+json"), result.getResponse().getContentType());
        Map<String, Object> body = JsonPath.read(result.getResponse().getContentAsString(), "$");
        Map<String, Object> schema = problemSchema();
        Set<String> allowed = ((Map<String, Object>) schema.get("properties")).keySet();
        for (String required : (List<String>) schema.get("required")) {
            assertTrue(body.containsKey(required), operationId + ": falta '" + required + "' em " + body);
        }
        for (String key : body.keySet()) {
            assertTrue(allowed.contains(key), operationId + ": campo fora do contrato '" + key + "'");
        }
        assertEquals(status, ((Number) body.get("status")).intValue());
        if (body.containsKey("errors")) {
            for (Map<String, Object> violation : (List<Map<String, Object>>) body.get("errors")) {
                assertFalse(violation.isEmpty());
                assertTrue(Set.of("field", "message").containsAll(violation.keySet()), violation.toString());
            }
        }
    }

    private MvcResult call(MockHttpServletRequestBuilder request, String token) throws Exception {
        return mvc.perform(token == null ? request : request.header(HttpHeaders.AUTHORIZATION, "Bearer " + token)).andReturn();
    }

    private static MockHttpServletRequestBuilder json(MockHttpServletRequestBuilder request, String body) {
        return request.contentType(MediaType.APPLICATION_JSON).content(body);
    }

    // ------------------------------------------------------------------ o percurso

    @Test
    void everyOperationAnswersWithinItsContract() throws Exception {
        String email = "contract." + UUID.randomUUID() + "@example.com";
        var origin = (org.springframework.test.web.servlet.request.RequestPostProcessor) r -> {
            r.setRemoteAddr("10.66." + (int) (Math.random() * 250) + "." + (int) (Math.random() * 250));
            return r;
        };

        // identity
        MvcResult registered = mvc.perform(json(post("/api/v1/auth/register").with(origin),
            "{\"email\":\"" + email + "\",\"password\":\"" + PASSWORD + "\"}")).andReturn();
        ok("registerUser", 201, registered, com.dowglasmaia.financecontroller.generated.identity.model.AuthResponse.class);
        problem("registerUser", 409, mvc.perform(json(post("/api/v1/auth/register").with(origin),
            "{\"email\":\"" + email + "\",\"password\":\"" + PASSWORD + "\"}")).andReturn());
        problem("registerUser", 400, mvc.perform(json(post("/api/v1/auth/register").with(origin), "{\"email\":\"x\",\"password\":\"1\"}")).andReturn());
        MvcResult login = mvc.perform(json(post("/api/v1/auth/login").with(origin),
            "{\"email\":\"" + email + "\",\"password\":\"" + PASSWORD + "\"}")).andReturn();
        ok("login", 200, login, com.dowglasmaia.financecontroller.generated.identity.model.AuthResponse.class);
        problem("login", 401, mvc.perform(json(post("/api/v1/auth/login").with(origin),
            "{\"email\":\"" + email + "\",\"password\":\"errada-errada\"}")).andReturn());
        String token = JsonPath.read(login.getResponse().getContentAsString(), "$.accessToken");
        Cookie refresh = login.getResponse().getCookie("refresh_token");
        ok("refreshToken", 200, mvc.perform(post("/api/v1/auth/refresh").cookie(refresh)).andReturn(),
            com.dowglasmaia.financecontroller.generated.identity.model.AccessTokenResponse.class);
        ok("getCurrentUser", 200, call(get("/api/v1/auth/me"), token), com.dowglasmaia.financecontroller.generated.identity.model.UserResponse.class);
        problem("getCurrentUser", 401, call(get("/api/v1/auth/me"), null));
        ok("requestPasswordReset", 202, mvc.perform(json(post("/api/v1/auth/password-reset/request").with(origin),
            "{\"email\":\"" + email + "\"}")).andReturn(), com.dowglasmaia.financecontroller.generated.identity.model.PasswordResetRequestedResponse.class);
        problem("confirmPasswordReset", 400, mvc.perform(json(post("/api/v1/auth/password-reset/confirm"),
            "{\"token\":\"inexistente\",\"newPassword\":\"" + PASSWORD + "x\"}")).andReturn());
        ok("logout", 204, mvc.perform(post("/api/v1/auth/logout").cookie(refresh)).andReturn(), null);

        // categories
        var categories = okList("listCategories", call(get("/api/v1/categories"), token),
            com.dowglasmaia.financecontroller.generated.categories.model.Category.class);
        assertEquals(11, categories.size());
        problem("listCategories", 401, call(get("/api/v1/categories"), null));
        var created = ok("createCategory", 201, call(json(post("/api/v1/categories"),
            "{\"name\":\"Contrato\",\"type\":\"EXPENSE\",\"icon\":\"sell\",\"color\":\"#FF9800\",\"subcategories\":[\"Um\"]}"), token),
            com.dowglasmaia.financecontroller.generated.categories.model.Category.class);
        problem("createCategory", 409, call(json(post("/api/v1/categories"),
            "{\"name\":\"contrato\",\"type\":\"EXPENSE\",\"icon\":\"sell\",\"color\":\"#FF9800\"}"), token));
        problem("createCategory", 400, call(json(post("/api/v1/categories"), "{\"name\":\"\"}"), token));
        UUID categoryId = created.getId();
        ok("getCategory", 200, call(get("/api/v1/categories/" + categoryId), token), com.dowglasmaia.financecontroller.generated.categories.model.Category.class);
        problem("getCategory", 404, call(get("/api/v1/categories/" + UUID.randomUUID()), token));
        ok("updateCategory", 200, call(json(put("/api/v1/categories/" + categoryId),
            "{\"name\":\"Contrato 2\",\"type\":\"EXPENSE\",\"icon\":\"sell\",\"color\":\"#FF9800\"}"), token),
            com.dowglasmaia.financecontroller.generated.categories.model.Category.class);
        var withSub = ok("addSubcategory", 201, call(json(post("/api/v1/categories/" + categoryId + "/subcategories"), "{\"name\":\"Dois\"}"), token),
            com.dowglasmaia.financecontroller.generated.categories.model.Subcategory.class);
        problem("addSubcategory", 409, call(json(post("/api/v1/categories/" + categoryId + "/subcategories"), "{\"name\":\"dois\"}"), token));
        ok("deleteSubcategory", 204, call(delete("/api/v1/categories/" + categoryId + "/subcategories/" + withSub.getId()), token), null);

        // transactions
        String tx = "{\"kind\":\"EXPENSE\",\"description\":\"Contrato\",\"amount\":\"120.50\",\"currency\":\"BRL\",\"date\":\"2026-06-10\","
            + "\"categoryId\":\"" + categoryId + "\",\"referenceMonth\":\"2026-06\"}";
        var result = ok("createTransaction", 201, call(json(post("/api/v1/transactions"), tx), token),
            com.dowglasmaia.financecontroller.generated.transactions.model.TransactionResult.class);
        problem("createTransaction", 400, call(json(post("/api/v1/transactions"), tx.replace("120.50", "-1")), token));
        problem("createTransaction", 404, call(json(post("/api/v1/transactions"), tx.replace(categoryId.toString(), UUID.randomUUID().toString())), token));
        UUID txId = result.getTransaction().getId();
        ok("listTransactions", 200, call(get("/api/v1/transactions?month=2026-06&currency=BRL"), token),
            com.dowglasmaia.financecontroller.generated.transactions.model.TransactionPage.class);
        problem("listTransactions", 400, call(get("/api/v1/transactions?month=2026-06"), token));
        ok("getTransaction", 200, call(get("/api/v1/transactions/" + txId), token),
            com.dowglasmaia.financecontroller.generated.transactions.model.Transaction.class);
        problem("getTransaction", 404, call(get("/api/v1/transactions/" + UUID.randomUUID()), token));
        ok("updateTransaction", 200, call(json(put("/api/v1/transactions/" + txId), tx.replace("120.50", "99.90")), token),
            com.dowglasmaia.financecontroller.generated.transactions.model.TransactionResult.class);

        // budgets
        ok("saveBudget", 200, call(json(put("/api/v1/budgets/BRL"),
            "{\"month\":\"2026-06\",\"monthlyLimit\":\"1000\",\"monthOverride\":{\"limit\":\"900\"},\"categoryLimits\":[{\"categoryId\":\""
                + categoryId + "\",\"limit\":\"200\"}]}"), token), com.dowglasmaia.financecontroller.generated.budgets.model.Budget.class);
        problem("saveBudget", 400, call(json(put("/api/v1/budgets/BRL"), "{\"month\":\"2026-13\",\"categoryLimits\":[]}"), token));
        problem("saveBudget", 404, call(json(put("/api/v1/budgets/BRL"),
            "{\"month\":\"2026-06\",\"categoryLimits\":[{\"categoryId\":\"" + UUID.randomUUID() + "\",\"limit\":\"1\"}]}"), token));
        ok("getBudget", 200, call(get("/api/v1/budgets/BRL?month=2026-06"), token), com.dowglasmaia.financecontroller.generated.budgets.model.Budget.class);
        ok("getBudgetProgress", 200, call(get("/api/v1/budgets/BRL/progress?month=2026-06"), token),
            com.dowglasmaia.financecontroller.generated.budgets.model.BudgetProgress.class);

        // money boxes e reservas
        var box = ok("createMoneyBox", 201, call(json(post("/api/v1/money-boxes"),
            "{\"name\":\"Cofre\",\"icon\":\"savings\",\"color\":\"#22C55E\",\"currency\":\"BRL\",\"goal\":\"500\"}"), token),
            com.dowglasmaia.financecontroller.generated.moneyboxes.model.MoneyBox.class);
        problem("createMoneyBox", 400, call(json(post("/api/v1/money-boxes"), "{\"name\":\"\"}"), token));
        okList("listMoneyBoxes", call(get("/api/v1/money-boxes?currency=BRL"), token), com.dowglasmaia.financecontroller.generated.moneyboxes.model.MoneyBox.class);
        ok("getMoneyBox", 200, call(get("/api/v1/money-boxes/" + box.getId()), token), com.dowglasmaia.financecontroller.generated.moneyboxes.model.MoneyBox.class);
        problem("getMoneyBox", 404, call(get("/api/v1/money-boxes/" + UUID.randomUUID()), token));
        var entry = ok("createMoneyBoxEntry", 201, call(json(post("/api/v1/money-boxes/" + box.getId() + "/entries"),
            "{\"kind\":\"DEPOSIT\",\"amount\":\"100\",\"date\":\"2026-06-10\"}"), token),
            com.dowglasmaia.financecontroller.generated.moneyboxes.model.MoneyBoxEntry.class);
        problem("createMoneyBoxEntry", 409, call(json(post("/api/v1/money-boxes/" + box.getId() + "/entries"),
            "{\"kind\":\"WITHDRAWAL\",\"amount\":\"1000\",\"date\":\"2026-06-11\"}"), token));
        okList("listMoneyBoxEntries", call(get("/api/v1/money-boxes/" + box.getId() + "/entries"), token),
            com.dowglasmaia.financecontroller.generated.moneyboxes.model.MoneyBoxEntry.class);
        ok("updateMoneyBoxEntry", 200, call(json(put("/api/v1/money-boxes/" + box.getId() + "/entries/" + entry.getId()),
            "{\"kind\":\"DEPOSIT\",\"amount\":\"150\",\"date\":\"2026-06-10\"}"), token),
            com.dowglasmaia.financecontroller.generated.moneyboxes.model.MoneyBoxEntry.class);
        problem("updateMoneyBox", 409, call(json(put("/api/v1/money-boxes/" + box.getId()),
            "{\"name\":\"Cofre\",\"icon\":\"savings\",\"color\":\"#22C55E\",\"currency\":\"EUR\"}"), token));
        ok("updateMoneyBox", 200, call(json(put("/api/v1/money-boxes/" + box.getId()),
            "{\"name\":\"Cofre 2\",\"icon\":\"savings\",\"color\":\"#22C55E\",\"currency\":\"BRL\"}"), token),
            com.dowglasmaia.financecontroller.generated.moneyboxes.model.MoneyBox.class);
        ok("getReservesSummary", 200, call(get("/api/v1/reserves/BRL/summary"), token),
            com.dowglasmaia.financecontroller.generated.reserves.model.ReservesSummary.class);
        ok("saveReserveGoal", 200, call(json(put("/api/v1/reserves/BRL/goal"), "{\"amount\":\"1000\"}"), token),
            com.dowglasmaia.financecontroller.generated.reserves.model.ReservesSummary.class);
        problem("saveReserveGoal", 400, call(json(put("/api/v1/reserves/BRL/goal"), "{\"amount\":\"-1\"}"), token));

        // dashboard, relatórios e preferências
        ok("getMonthlyOverview", 200, call(get("/api/v1/overview?month=2026-06&currency=BRL"), token),
            com.dowglasmaia.financecontroller.generated.overview.model.MonthlyOverview.class);
        problem("getMonthlyOverview", 400, call(get("/api/v1/overview?month=x&currency=BRL"), token));
        for (String period : List.of("WEEK", "MONTH", "YEAR")) {
            ok("getReport", 200, call(get("/api/v1/reports?period=" + period + "&anchorDate=2026-06-15&currency=BRL"), token),
                com.dowglasmaia.financecontroller.generated.reports.model.Report.class);
        }
        problem("getReport", 400, call(get("/api/v1/reports?period=DAY&anchorDate=2026-06-15&currency=BRL"), token));
        ok("getPreferences", 200, call(get("/api/v1/preferences"), token), com.dowglasmaia.financecontroller.generated.preferences.model.Preferences.class);
        ok("savePreferences", 200, call(json(put("/api/v1/preferences"), "{\"currency\":\"EUR\",\"theme\":\"SYSTEM\"}"), token),
            com.dowglasmaia.financecontroller.generated.preferences.model.Preferences.class);
        problem("savePreferences", 400, call(json(put("/api/v1/preferences"), "{\"currency\":\"USD\",\"theme\":\"DARK\"}"), token));

        // exclusões por último (dependem dos dados acima)
        ok("deleteMoneyBoxEntry", 204, call(delete("/api/v1/money-boxes/" + box.getId() + "/entries/" + entry.getId()), token), null);
        ok("deleteMoneyBox", 204, call(delete("/api/v1/money-boxes/" + box.getId()), token), null);
        problem("deleteMoneyBox", 404, call(delete("/api/v1/money-boxes/" + box.getId()), token));
        problem("deleteCategory", 409, call(delete("/api/v1/categories/" + categoryId), token));
        ok("deleteTransaction", 204, call(delete("/api/v1/transactions/" + txId), token), null);
        problem("deleteTransaction", 404, call(delete("/api/v1/transactions/" + txId), token));
        ok("deleteCategory", 204, call(delete("/api/v1/categories/" + categoryId), token), null);

        Set<String> all = declaredStatuses().keySet();
        Set<String> missing = new TreeSet<>(all);
        missing.removeAll(exercised);
        assertTrue(missing.isEmpty(), "operações do contrato sem teste de conformidade: " + missing);
        assertEquals(all.size(), exercised.stream().filter(all::contains).count());
    }
}
