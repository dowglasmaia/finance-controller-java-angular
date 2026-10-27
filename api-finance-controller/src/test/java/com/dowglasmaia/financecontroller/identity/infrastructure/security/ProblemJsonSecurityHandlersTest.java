package com.dowglasmaia.financecontroller.identity.infrastructure.security;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.dowglasmaia.financecontroller.shared.error.ErrorHandlingConfiguration;
import com.dowglasmaia.financecontroller.shared.error.ProblemResponseWriter;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.oauth2.server.resource.InvalidBearerTokenException;
import tools.jackson.databind.json.JsonMapper;

class ProblemJsonSecurityHandlersTest {
    private final JsonMapper mapper = JsonMapper.builder().build();
    private final ProblemResponseWriter writer =
        new ProblemResponseWriter(new ErrorHandlingConfiguration().messageSource(), mapper);
    private final ProblemJsonSecurityHandlers handlers = new ProblemJsonSecurityHandlers(writer);

    @SuppressWarnings("unchecked")
    private Map<String, Object> body(MockHttpServletResponse response) throws Exception {
        return mapper.readValue(response.getContentAsByteArray(), Map.class);
    }

    @Test
    void entryPointWritesUnauthenticatedProblemWithoutInternalDetail() throws Exception {
        var request = new MockHttpServletRequest("GET", "/api/v1/categories");
        var response = new MockHttpServletResponse();

        handlers.commence(request, response, new InvalidBearerTokenException("Jwt expired at 2026-01-01T00:00:00Z"));

        assertEquals(401, response.getStatus());
        assertTrue(response.getContentType().startsWith("application/problem+json"));
        assertEquals("Bearer", response.getHeader("WWW-Authenticate"));
        Map<String, Object> body = body(response);
        assertEquals("UNAUTHENTICATED", body.get("code"));
        assertEquals(401, body.get("status"));
        assertEquals("Sessão inválida ou expirada.", body.get("detail"));
        assertEquals("Não autenticado", body.get("title"));
        assertEquals("urn:finance-controller:problem:unauthenticated", body.get("type"));
        assertEquals("/api/v1/categories", body.get("instance"));
        String raw = response.getContentAsString();
        assertFalse(raw.contains("expired"));
        assertFalse(raw.contains("Jwt"));
        assertFalse(raw.contains("Exception"));
    }

    @Test
    void accessDeniedAlsoAnswersUnauthenticated() throws Exception {
        var response = new MockHttpServletResponse();

        handlers.handle(new MockHttpServletRequest("GET", "/api/v1/x"), response, new AccessDeniedException("secret reason"));

        assertEquals(401, response.getStatus());
        assertEquals("UNAUTHENTICATED", body(response).get("code"));
        assertFalse(response.getContentAsString().contains("secret reason"));
    }

    @Test
    void authenticationExceptionMessageIsNeverEchoed() throws Exception {
        var response = new MockHttpServletResponse();

        handlers.commence(new MockHttpServletRequest("POST", "/x"), response, new BadCredentialsException("bad password hunter2"));

        assertFalse(response.getContentAsString().contains("hunter2"));
    }

    @Test
    void bodySizeFilterRejectsOversizedAndPassesOthers() throws Exception {
        var filter = new RequestBodySizeLimitFilter(10, writer);

        var big = new MockHttpServletRequest("POST", "/api/v1/auth/login");
        big.setContent(new byte[11]);
        var rejected = new MockHttpServletResponse();
        var chain = new MockFilterChain();
        filter.doFilter(big, rejected, chain);
        assertEquals(413, rejected.getStatus());
        assertEquals("REQUEST_NOT_SUPPORTED", body(rejected).get("code"));
        assertNull(chain.getRequest());

        var small = new MockHttpServletRequest("POST", "/api/v1/auth/login");
        small.setContent(new byte[10]);
        var okChain = new MockFilterChain();
        filter.doFilter(small, new MockHttpServletResponse(), okChain);
        assertEquals(small, okChain.getRequest());
    }
}
