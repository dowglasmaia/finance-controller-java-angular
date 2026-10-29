package com.dowglasmaia.financecontroller.category;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;

import com.dowglasmaia.financecontroller.support.AbstractPostgresIntegrationTest;
import com.jayway.jsonpath.JsonPath;
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
 * Categories through the real HTTP stack and a real PostgreSQL (Testcontainers): seed on registration, CRUD,
 * uniqueness (including a concurrent race), protected "Outros", error codes and isolation between users.
 */
@AutoConfigureMockMvc
class CategoryFlowIntegrationTest extends AbstractPostgresIntegrationTest {
    private static final String PASSWORD = "Sup3r-Secret-Passw0rd";

    @DynamicPropertySource
    static void identityProperties(DynamicPropertyRegistry registry) {
        registry.add("app.identity.jwt.secret", () -> "test-only-dummy-jwt-secret-0123456789-abcdef");
    }

    @Autowired
    private MockMvc mvc;

    /** Registers a new user and returns its access token. */
    private String newUserToken() throws Exception {
        String email = "cat." + UUID.randomUUID() + "@example.com";
        MvcResult result = mvc.perform(post("/api/v1/auth/register").with(r -> {
            r.setRemoteAddr("10.9." + (int) (Math.random() * 250) + "." + (int) (Math.random() * 250));
            return r;
        }).contentType(MediaType.APPLICATION_JSON)
            .content("{\"email\":\"" + email + "\",\"password\":\"" + PASSWORD + "\"}")).andReturn();
        assertEquals(201, result.getResponse().getStatus());
        return JsonPath.read(result.getResponse().getContentAsString(), "$.accessToken");
    }

    private static MockHttpServletRequestBuilder auth(MockHttpServletRequestBuilder request, String token) {
        return request.header(HttpHeaders.AUTHORIZATION, "Bearer " + token);
    }

    private static String body(String name, String type) {
        return "{\"name\":\"" + name + "\",\"type\":\"" + type + "\",\"icon\":\"home\",\"color\":\"#112233\"}";
    }

    private MvcResult call(MockHttpServletRequestBuilder request, String token) throws Exception {
        return mvc.perform(auth(request, token)).andReturn();
    }

    private MvcResult create(String token, String json) throws Exception {
        return call(post("/api/v1/categories").contentType(MediaType.APPLICATION_JSON).content(json), token);
    }

    private static String text(MvcResult r) throws Exception {
        return r.getResponse().getContentAsString();
    }

    private static String code(MvcResult r) throws Exception {
        return JsonPath.read(text(r), "$.code");
    }

    private List<String> names(String token, String query) throws Exception {
        return JsonPath.read(text(call(get("/api/v1/categories" + query), token)), "$[*].name");
    }

    @Test
    void registrationSeedsElevenCategoriesAndOtherUsersDoNotSeeThem() throws Exception {
        String ana = newUserToken();
        String rui = newUserToken();

        List<String> anaNames = names(ana, "");
        assertEquals(11, anaNames.size());
        assertEquals(7, names(ana, "?type=EXPENSE").size());
        assertEquals(4, names(ana, "?type=INCOME").size());

        // outro utilizador cria o mesmo nome de uma categoria semeada: nomes são únicos por utilizador, não globais
        MvcResult ruiLists = call(get("/api/v1/categories"), rui);
        assertEquals(11, ((List<?>) JsonPath.read(text(ruiLists), "$")).size());
        List<String> ruiAlimentacaoIds = JsonPath.read(text(ruiLists), "$[?(@.name=='Alimentação')].id");
        String ruiAlimentacao = ruiAlimentacaoIds.get(0);
        assertEquals(404, call(get("/api/v1/categories/" + ruiAlimentacao), ana).getResponse().getStatus());
        assertEquals(404, call(delete("/api/v1/categories/" + ruiAlimentacao), ana).getResponse().getStatus());
        assertEquals(404, call(put("/api/v1/categories/" + ruiAlimentacao).contentType(MediaType.APPLICATION_JSON)
            .content(body("Hack", "EXPENSE")), ana).getResponse().getStatus());
        assertTrue(names(rui, "").contains("Alimentação"));
    }

    @Test
    void requiresAuthentication() throws Exception {
        assertEquals(401, mvc.perform(get("/api/v1/categories")).andReturn().getResponse().getStatus());
    }

    @Test
    void createAlwaysAddsProtectedOthersAndListIsSortedAndFiltered() throws Exception {
        String token = newUserToken();

        MvcResult created = create(token,
            "{\"name\":\"Zumba\",\"type\":\"BOTH\",\"icon\":\"fitness\",\"color\":\"#AABBCC\",\"subcategories\":[\"Aulas\",\"outros\"]}");
        assertEquals(201, created.getResponse().getStatus());
        List<String> subs = JsonPath.read(text(created), "$.subcategories[*].name");
        assertEquals(List.of("Outros", "Aulas"), subs);
        assertEquals(List.of(true, false), JsonPath.read(text(created), "$.subcategories[*].protected"));

        List<String> expense = names(token, "?type=EXPENSE");
        assertTrue(expense.contains("Zumba"));
        assertTrue(names(token, "?type=INCOME").contains("Zumba"));
        assertEquals(expense.stream().sorted(String.CASE_INSENSITIVE_ORDER).count(), expense.size());
        assertEquals(400, call(get("/api/v1/categories?type=BOTH"), token).getResponse().getStatus());
    }

    @Test
    void duplicateNamesAreRejectedIgnoringCaseAndSpaces() throws Exception {
        String token = newUserToken();

        MvcResult duplicate = create(token, body("  ALIMENTAÇÃO ", "EXPENSE"));
        assertEquals(409, duplicate.getResponse().getStatus());
        assertEquals("CATEGORY_NAME_DUPLICATED", code(duplicate));
        assertEquals(400, create(token, body("", "EXPENSE")).getResponse().getStatus());
        assertEquals(400, create(token,
            "{\"name\":\"X\",\"type\":\"EXPENSE\",\"icon\":\"i\",\"color\":\"red\"}").getResponse().getStatus());
    }

    @Test
    void concurrentCreationsWithTheSameNameYieldOneCreatedAndOneConflict() throws Exception {
        String token = newUserToken();
        String json = body("Corrida " + UUID.randomUUID(), "EXPENSE");
        var start = new CountDownLatch(1);
        var pool = Executors.newFixedThreadPool(2);
        Callable<Integer> task = () -> {
            start.await();
            return create(token, json).getResponse().getStatus();
        };
        var first = pool.submit(task);
        var second = pool.submit(task);
        start.countDown();
        List<Integer> statuses = List.of(first.get(), second.get());
        pool.shutdown();

        assertEquals(1, statuses.stream().filter(s -> s == 201).count(), statuses.toString());
        assertEquals(1, statuses.stream().filter(s -> s == 409).count(), statuses.toString());
    }

    @Test
    void updateChangesFieldsAndKeepsSubcategories() throws Exception {
        String token = newUserToken();
        MvcResult created = create(token, body("Pets", "EXPENSE"));
        String id = JsonPath.read(text(created), "$.id");

        MvcResult updated = call(put("/api/v1/categories/" + id).contentType(MediaType.APPLICATION_JSON)
            .content("{\"name\":\"Animais\",\"type\":\"BOTH\",\"icon\":\"pets\",\"color\":\"#FF0000\"}"), token);
        assertEquals(200, updated.getResponse().getStatus());
        assertEquals("Animais", JsonPath.read(text(updated), "$.name"));
        assertEquals("BOTH", JsonPath.read(text(updated), "$.type"));
        assertEquals(List.of("Outros"), JsonPath.read(text(updated), "$.subcategories[*].name"));

        MvcResult clash = call(put("/api/v1/categories/" + id).contentType(MediaType.APPLICATION_JSON)
            .content(body("carro", "EXPENSE")), token);
        assertEquals(409, clash.getResponse().getStatus());
        assertEquals("CATEGORY_NAME_DUPLICATED", code(clash));
    }

    @Test
    void subcategoriesCanBeAddedAndRemovedButOthersIsProtected() throws Exception {
        String token = newUserToken();
        MvcResult created = create(token, body("Viagens", "EXPENSE"));
        String id = JsonPath.read(text(created), "$.id");
        String othersId = JsonPath.read(text(created), "$.subcategories[0].id");

        MvcResult added = call(post("/api/v1/categories/" + id + "/subcategories")
            .contentType(MediaType.APPLICATION_JSON).content("{\"name\":\"Hotel\"}"), token);
        assertEquals(201, added.getResponse().getStatus());
        String hotelId = JsonPath.read(text(added), "$.id");

        MvcResult duplicate = call(post("/api/v1/categories/" + id + "/subcategories")
            .contentType(MediaType.APPLICATION_JSON).content("{\"name\":\" hotel \"}"), token);
        assertEquals(409, duplicate.getResponse().getStatus());
        assertEquals("SUBCATEGORY_NAME_DUPLICATED", code(duplicate));

        MvcResult protectedDelete = call(delete("/api/v1/categories/" + id + "/subcategories/" + othersId), token);
        assertEquals(409, protectedDelete.getResponse().getStatus());
        assertEquals("SUBCATEGORY_PROTECTED", code(protectedDelete));

        assertEquals(204, call(delete("/api/v1/categories/" + id + "/subcategories/" + hotelId), token).getResponse().getStatus());
        assertEquals(404, call(delete("/api/v1/categories/" + id + "/subcategories/" + hotelId), token).getResponse().getStatus());
        assertEquals(List.of("Outros"),
            JsonPath.read(text(call(get("/api/v1/categories/" + id), token)), "$.subcategories[*].name"));
    }

    @Test
    void deleteRemovesCategoryAndItsSubcategories() throws Exception {
        String token = newUserToken();
        MvcResult created = create(token,
            "{\"name\":\"Temporária\",\"type\":\"EXPENSE\",\"icon\":\"i\",\"color\":\"#000000\",\"subcategories\":[\"A\",\"B\"]}");
        String id = JsonPath.read(text(created), "$.id");

        assertEquals(204, call(delete("/api/v1/categories/" + id), token).getResponse().getStatus());
        assertEquals(404, call(get("/api/v1/categories/" + id), token).getResponse().getStatus());
        assertEquals(11, names(token, "").size());
    }
}
