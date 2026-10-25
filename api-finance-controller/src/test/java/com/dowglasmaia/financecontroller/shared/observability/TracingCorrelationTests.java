package com.dowglasmaia.financecontroller.shared.observability;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import io.micrometer.tracing.Tracer;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

/** Guards the wiring every log line relies on: an active span puts traceId into the MDC. */
@SpringBootTest
class TracingCorrelationTests {
    @Autowired
    Tracer tracer;

    @Test
    void activeSpanExposesTraceIdInMdc() {
        var span = tracer.nextSpan().name("correlation-check").start();
        try (var ignored = tracer.withSpan(span)) {
            assertNotNull(MDC.get("traceId"));
        } finally {
            span.end();
        }
        assertNull(MDC.get("traceId"));
    }
}
