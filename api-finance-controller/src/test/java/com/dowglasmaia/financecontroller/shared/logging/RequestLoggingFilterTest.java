package com.dowglasmaia.financecontroller.shared.logging;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

class RequestLoggingFilterTest {
    private final RequestLoggingFilter filter = new RequestLoggingFilter();

    private String run(String incomingId, AtomicReference<String> mdcInsideChain) throws Exception {
        var request = new MockHttpServletRequest("GET", "/api/v1/examples/1");
        if (incomingId != null) request.addHeader(RequestLoggingFilter.HEADER, incomingId);
        var response = new MockHttpServletResponse();
        filter.doFilter(request, response, (req, res) -> mdcInsideChain.set(MDC.get(RequestLoggingFilter.MDC_KEY)));
        return response.getHeader(RequestLoggingFilter.HEADER);
    }

    @Test
    void reusesValidRequestIdAndClearsMdc() throws Exception {
        var inside = new AtomicReference<String>();
        assertEquals("abc-123", run("abc-123", inside));
        assertEquals("abc-123", inside.get());
        assertNull(MDC.get(RequestLoggingFilter.MDC_KEY));
    }

    @Test
    void generatesIdWhenMissingOrUnsafe() throws Exception {
        var inside = new AtomicReference<String>();
        var generated = run(null, inside);
        assertEquals(generated, inside.get());
        assertNotEquals("bad id\nforged", run("bad id\nforged", inside));
    }
}
