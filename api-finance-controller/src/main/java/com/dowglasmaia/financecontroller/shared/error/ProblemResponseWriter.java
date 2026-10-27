package com.dowglasmaia.financecontroller.shared.error;

import com.dowglasmaia.financecontroller.shared.logging.RequestLoggingFilter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import org.slf4j.MDC;
import org.springframework.context.MessageSource;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

/**
 * Writes the same {@code application/problem+json} body as {@link GlobalExceptionHandler} for failures raised before
 * Spring MVC (servlet filters, Spring Security), where {@code @RestControllerAdvice} does not apply.
 */
@Component
public class ProblemResponseWriter {
    private static final String PROBLEM_TYPE_PREFIX = "urn:finance-controller:problem:";

    private final MessageSource messages;
    private final ObjectMapper mapper;

    public ProblemResponseWriter(MessageSource messages, ObjectMapper mapper) {
        this.messages = messages;
        this.mapper = mapper;
    }

    public void write(HttpServletRequest request, HttpServletResponse response, HttpStatus status, ErrorCode code)
            throws IOException {
        var body = new LinkedHashMap<String, Object>();
        body.put("type", PROBLEM_TYPE_PREFIX + code.name().toLowerCase(Locale.ROOT).replace('_', '-'));
        body.put("title", messages.getMessage("problem.title." + status.value(), null, status.getReasonPhrase(),
            ErrorHandlingConfiguration.PT_BR));
        body.put("status", status.value());
        body.put("detail", messages.getMessage(code.messageKey(), new Object[0], ErrorHandlingConfiguration.PT_BR));
        body.put("instance", request.getRequestURI().replaceAll("[^A-Za-z0-9/._~-]", ""));
        body.put("code", code.name());
        putIfPresent(body, "traceId", MDC.get("traceId"));
        putIfPresent(body, "correlationId", MDC.get(RequestLoggingFilter.MDC_KEY));

        response.setStatus(status.value());
        response.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        response.getOutputStream().write(mapper.writeValueAsBytes(body));
    }

    private static void putIfPresent(Map<String, Object> body, String name, String value) {
        if (value != null && !value.isBlank()) {
            body.put(name, value);
        }
    }
}
