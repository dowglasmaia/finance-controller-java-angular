package com.dowglasmaia.financecontroller.shared.error;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.slf4j.MDC;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;

/** Standalone MockMvc: no Spring context, no Docker. Covers every catalog code (BACKEND-SPEC-04). */
class GlobalExceptionHandlerTest {
    /** Texts copied from the spec 10 catalog; the properties file must reproduce them exactly. */
    private static final Map<ErrorCode, String> EXPECTED_DETAIL = new EnumMap<>(ErrorCode.class);

    static {
        EXPECTED_DETAIL.put(ErrorCode.VALIDATION_FAILED, "Há campos inválidos. Corrija-os e tente novamente.");
        EXPECTED_DETAIL.put(ErrorCode.RESOURCE_NOT_FOUND, "Recurso não encontrado.");
        EXPECTED_DETAIL.put(ErrorCode.CATEGORY_NAME_DUPLICATED, "Já existe uma categoria com este nome.");
        EXPECTED_DETAIL.put(ErrorCode.CATEGORY_IN_USE, "Categoria possui lançamentos vinculados e não pode ser excluída.");
        EXPECTED_DETAIL.put(ErrorCode.CATEGORY_TYPE_MISMATCH, "A categoria não aceita este tipo de lançamento.");
        EXPECTED_DETAIL.put(ErrorCode.SUBCATEGORY_PROTECTED, "A subcategoria \"Outros\" não pode ser excluída.");
        EXPECTED_DETAIL.put(ErrorCode.SUBCATEGORY_NAME_DUPLICATED, "Já existe uma subcategoria com este nome nesta categoria.");
        EXPECTED_DETAIL.put(ErrorCode.SUBCATEGORY_NOT_IN_CATEGORY, "A subcategoria não pertence à categoria escolhida.");
        EXPECTED_DETAIL.put(ErrorCode.CATEGORY_TYPE_CONFLICT, "Existem lançamentos incompatíveis com o novo tipo desta categoria.");
        EXPECTED_DETAIL.put(ErrorCode.UNAUTHENTICATED, "Sessão inválida ou expirada.");
        EXPECTED_DETAIL.put(ErrorCode.INVALID_CREDENTIALS, "E-mail ou senha inválidos.");
        EXPECTED_DETAIL.put(ErrorCode.EMAIL_ALREADY_REGISTERED, "Este e-mail já está cadastrado.");
        EXPECTED_DETAIL.put(ErrorCode.TOO_MANY_ATTEMPTS, "Muitas tentativas. Aguarde e tente novamente.");
        EXPECTED_DETAIL.put(ErrorCode.PASSWORD_RESET_TOKEN_INVALID, "Este link é inválido ou expirou. Peça um novo.");
        EXPECTED_DETAIL.put(ErrorCode.CURRENCY_UNSUPPORTED, "Moeda não suportada: XYZ.");
        EXPECTED_DETAIL.put(ErrorCode.CURRENCY_MISMATCH, "A moeda do aporte (EUR) deve ser igual à moeda do cofrinho (BRL).");
        EXPECTED_DETAIL.put(ErrorCode.CURRENCY_LOCKED, "Não é possível alterar a moeda de um cofrinho com aportes.");
        EXPECTED_DETAIL.put(ErrorCode.MONEY_BOX_NOT_FOUND, "Cofrinho não encontrado.");
        EXPECTED_DETAIL.put(ErrorCode.INSUFFICIENT_BALANCE,
            "Saldo insuficiente: o saque excede o saldo do cofrinho (ou o saldo é zero).");
        EXPECTED_DETAIL.put(ErrorCode.INTERNAL_ERROR, "Erro inesperado. Tente novamente.");
        EXPECTED_DETAIL.put(ErrorCode.REQUEST_NOT_SUPPORTED, "Requisição não suportada.");
    }

    private static final Pattern STACK_FRAME = Pattern.compile("\\sat\\s[\\w.$]+\\(");
    private static final List<String> FORBIDDEN = List.of("Exception", "SELECT", "org.", "java.", "com.dowglasmaia",
        "uk_test_name", "fk_test_category", "ck_secret_internal", "secret", "constraint", "duplicate key", "Detail:",
        "stackTrace");

    private MockMvc mvc;

    @BeforeEach
    void setUp() {
        var messages = new ErrorHandlingConfiguration().messageSource();
        var validator = new LocalValidatorFactoryBean();
        validator.setValidationMessageSource(messages);
        validator.afterPropertiesSet();
        var registry = new ConstraintErrorRegistry(List.of(
            new ConstraintErrorMapping("uk_test_name", ErrorCode.CATEGORY_NAME_DUPLICATED),
            new ConstraintErrorMapping("FK_TEST_CATEGORY", ErrorCode.CATEGORY_IN_USE)));
        mvc = MockMvcBuilders.standaloneSetup(new ErrorProbeController(validator))
            .setControllerAdvice(new GlobalExceptionHandler(messages, registry))
            .setValidator(validator)
            .build();
    }

    @AfterEach
    void clearMdc() {
        MDC.clear();
    }

    @Test
    void catalogHasAnExpectedTextForEveryCode() {
        assertEquals(List.of(ErrorCode.values()), List.copyOf(EXPECTED_DETAIL.keySet()));
    }

    @ParameterizedTest
    @EnumSource(ErrorCode.class)
    void everyCatalogCodeAnswersWithItsStatusAndMessage(ErrorCode code) throws Exception {
        problem(get("/probe/domain/{code}", code.name()), code.status().value(), code)
            .andExpect(jsonPath("$.detail").value(EXPECTED_DETAIL.get(code)))
            .andExpect(jsonPath("$.status").value(code.status().value()))
            .andExpect(jsonPath("$.title").isNotEmpty())
            .andExpect(jsonPath("$.type").value("urn:finance-controller:problem:" + code.name().toLowerCase().replace('_', '-')));
    }

    @Test
    void resourceOfAnotherUserIsNotFoundNeverForbidden() throws Exception {
        problem(get("/probe/foreign-resource"), 404, ErrorCode.RESOURCE_NOT_FOUND)
            .andExpect(jsonPath("$.detail").value("Recurso não encontrado."));
    }

    @Test
    void uniqueViolationIsTranslatedByConstraintName() throws Exception {
        problem(get("/probe/unique"), 409, ErrorCode.CATEGORY_NAME_DUPLICATED)
            .andExpect(jsonPath("$.detail").value("Já existe uma categoria com este nome."));
    }

    @Test
    void foreignKeyViolationIsTranslatedIgnoringCase() throws Exception {
        problem(get("/probe/foreign-key"), 409, ErrorCode.CATEGORY_IN_USE)
            .andExpect(jsonPath("$.detail").value("Categoria possui lançamentos vinculados e não pode ser excluída."));
    }

    @Test
    void unmappedConstraintIsAGenericInternalError() throws Exception {
        problem(get("/probe/unmapped-integrity"), 500, ErrorCode.INTERNAL_ERROR)
            .andExpect(jsonPath("$.detail").value("Erro inesperado. Tente novamente."));
    }

    @Test
    void unexpectedFailureIsAGenericInternalError() throws Exception {
        problem(get("/probe/boom"), 500, ErrorCode.INTERNAL_ERROR)
            .andExpect(jsonPath("$.detail").value("Erro inesperado. Tente novamente."));
    }

    @Test
    void invalidBodyListsEveryFieldWithItsMessage() throws Exception {
        problem(post("/probe/body").contentType(MediaType.APPLICATION_JSON).content("{\"name\":\" \"}"), 400,
            ErrorCode.VALIDATION_FAILED)
            .andExpect(jsonPath("$.errors.length()").value(2))
            .andExpect(jsonPath("$.errors[?(@.field=='name')].message").value("Campo obrigatório"))
            .andExpect(jsonPath("$.errors[?(@.field=='amount')].message").value("Informe um valor"));
    }

    @Test
    void nonPositiveAmountUsesTheSpecMessage() throws Exception {
        problem(post("/probe/body").contentType(MediaType.APPLICATION_JSON).content("{\"name\":\"x\",\"amount\":0}"), 400,
            ErrorCode.VALIDATION_FAILED)
            .andExpect(jsonPath("$.errors[0].field").value("amount"))
            .andExpect(jsonPath("$.errors[0].message").value("O valor deve ser maior que zero"));
    }

    @Test
    void invalidRequestParameterIsAValidationFailure() throws Exception {
        problem(get("/probe/param").param("amount", "-1"), 400, ErrorCode.VALIDATION_FAILED)
            .andExpect(jsonPath("$.errors[0].field").value("amount"))
            .andExpect(jsonPath("$.errors[0].message").value("O valor deve ser maior que zero"));
    }

    @Test
    void constraintViolationExceptionIsAValidationFailure() throws Exception {
        problem(get("/probe/constraint-violation"), 400, ErrorCode.VALIDATION_FAILED)
            .andExpect(jsonPath("$.errors[0].field").value("name"))
            .andExpect(jsonPath("$.errors[0].message").value("Campo obrigatório"));
    }

    @Test
    void unreadableBodyIsAValidationFailureWithoutParserDetails() throws Exception {
        problem(post("/probe/body").contentType(MediaType.APPLICATION_JSON).content("{not json"), 400,
            ErrorCode.VALIDATION_FAILED)
            .andExpect(jsonPath("$.errors[0].field").value("body"))
            .andExpect(jsonPath("$.errors[0].message").value("Valor inválido"));
    }

    @Test
    void frameworkRejectionKeepsItsStatusWithCatalogText() throws Exception {
        problem(post("/probe/boom"), 405, ErrorCode.REQUEST_NOT_SUPPORTED)
            .andExpect(jsonPath("$.detail").value("Requisição não suportada."));
    }

    @Test
    void responseCarriesTraceAndCorrelationIdsWhenPresent() throws Exception {
        MDC.put("traceId", "4bf92f3577b34da6a3ce929d0e0e4736");
        MDC.put("requestId", "req-123");
        problem(get("/probe/boom"), 500, ErrorCode.INTERNAL_ERROR)
            .andExpect(jsonPath("$.traceId").value("4bf92f3577b34da6a3ce929d0e0e4736"))
            .andExpect(jsonPath("$.correlationId").value("req-123"));
    }

    @Test
    void idsAreOmittedWhenAbsent() throws Exception {
        problem(get("/probe/boom"), 500, ErrorCode.INTERNAL_ERROR)
            .andExpect(jsonPath("$.traceId").doesNotExist())
            .andExpect(jsonPath("$.correlationId").doesNotExist());
    }

    @Test
    void noErrorResponseExposesStackSqlOrInternalNames() throws Exception {
        var requests = List.of(
            get("/probe/unique"), get("/probe/foreign-key"), get("/probe/unmapped-integrity"), get("/probe/boom"),
            get("/probe/foreign-resource"), get("/probe/constraint-violation"), get("/probe/param").param("amount", "-1"),
            post("/probe/boom"),
            post("/probe/body").contentType(MediaType.APPLICATION_JSON).content("{not json"),
            post("/probe/body").contentType(MediaType.APPLICATION_JSON).content("{\"name\":\" \"}"));
        for (var request : requests) {
            String body = mvc.perform(request).andReturn().getResponse().getContentAsString()
                .replaceFirst("\"instance\":\"[^\"]*\",?", "");
            assertFalse(body.isBlank(), "error responses must have a body");
            for (String forbidden : FORBIDDEN) {
                assertFalse(body.contains(forbidden), "response leaks '" + forbidden + "': " + body);
            }
            assertFalse(STACK_FRAME.matcher(body).find(), "response contains a stack frame: " + body);
        }
        for (ErrorCode code : ErrorCode.values()) {
            String body = mvc.perform(get("/probe/domain/{code}", code.name())).andReturn().getResponse().getContentAsString();
            assertTrue(body.contains("\"code\":\"" + code.name() + "\""));
            assertFalse(STACK_FRAME.matcher(body).find());
        }
    }

    private ResultActions problem(org.springframework.test.web.servlet.RequestBuilder request, int status, ErrorCode code)
            throws Exception {
        return mvc.perform(request)
            .andExpect(status().is(status))
            .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
            .andExpect(jsonPath("$.code").value(code.name()));
    }
}
