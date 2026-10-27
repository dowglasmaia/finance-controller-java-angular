package com.dowglasmaia.financecontroller.shared.error;

import com.dowglasmaia.financecontroller.shared.logging.RequestLoggingFilter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Path;
import java.net.URI;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.context.MessageSource;
import org.springframework.context.MessageSourceResolvable;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.validation.ObjectError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

/**
 * Turns every failure into an RFC 9457 {@code application/problem+json} body with a catalog {@code code}
 * (BACKEND-SPEC-04). Responses carry only catalog text: never a stack trace, SQL, constraint or class names.
 * Details of unexpected failures go to the log, correlated by {@code requestId}/{@code traceId}.
 */
@RestControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {
    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);
    private static final Pattern CONSTRAINT_NAME = Pattern.compile("constraint \"([^\"]+)\"", Pattern.CASE_INSENSITIVE);
    private static final String PROBLEM_TYPE_PREFIX = "urn:finance-controller:problem:";
    private static final String CODE = "code";
    private static final String ERRORS = "errors";
    private static final String INVALID_VALUE_KEY = "validation.invalid";

    private final MessageSource messages;
    private final ConstraintErrorRegistry constraints;

    public GlobalExceptionHandler(MessageSource messages, ConstraintErrorRegistry constraints) {
        this.messages = messages;
        this.constraints = constraints;
        setMessageSource(messages);
    }

    @ExceptionHandler(DomainException.class)
    ResponseEntity<Object> handleDomain(DomainException ex, WebRequest request) {
        log.atInfo().addKeyValue("error.code", ex.code()).log("domain rule rejected the request");
        return respond(problem(ex.code(), ex.code().status(), ex.args(), List.of(), request));
    }

    @ExceptionHandler(ConstraintViolationException.class)
    ResponseEntity<Object> handleConstraintViolation(ConstraintViolationException ex, WebRequest request) {
        var errors = ex.getConstraintViolations().stream()
            .map(v -> new FieldViolation(lastNode(v.getPropertyPath()), v.getMessage()))
            .toList();
        return respond(validationProblem(errors, request));
    }

    /** Catch-all: translates database integrity violations by constraint name, everything else is a generic 500. */
    @ExceptionHandler(Exception.class)
    ResponseEntity<Object> handleUnexpected(Exception ex, WebRequest request) {
        if (isIntegrityViolation(ex)) {
            Optional<ErrorCode> mapped = constraintName(ex).flatMap(constraints::find);
            if (mapped.isPresent()) {
                log.atInfo().addKeyValue("error.code", mapped.get()).log("database constraint translated");
                return respond(problem(mapped.get(), mapped.get().status(), new Object[0], List.of(), request));
            }
            // An unmapped constraint is a missing mapping (programming error), not something the client can fix.
            log.error("unmapped integrity violation", ex);
        } else {
            log.error("unexpected failure", ex);
        }
        return respond(problem(ErrorCode.INTERNAL_ERROR, HttpStatus.INTERNAL_SERVER_ERROR, new Object[0], List.of(), request));
    }

    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(MethodArgumentNotValidException ex, HttpHeaders headers,
            HttpStatusCode status, WebRequest request) {
        var errors = new ArrayList<FieldViolation>();
        for (ObjectError error : ex.getBindingResult().getAllErrors()) {
            String field = error instanceof FieldError fieldError ? fieldError.getField() : error.getObjectName();
            errors.add(new FieldViolation(field, error.getDefaultMessage()));
        }
        return respond(validationProblem(errors, request));
    }

    @Override
    protected ResponseEntity<Object> handleHandlerMethodValidationException(HandlerMethodValidationException ex,
            HttpHeaders headers, HttpStatusCode status, WebRequest request) {
        var errors = new ArrayList<FieldViolation>();
        ex.getParameterValidationResults().forEach(result -> {
            String parameter = Objects.requireNonNullElse(result.getMethodParameter().getParameterName(), "parameter");
            for (MessageSourceResolvable error : result.getResolvableErrors()) {
                String field = error instanceof FieldError fieldError ? fieldError.getField() : parameter;
                errors.add(new FieldViolation(field, error.getDefaultMessage()));
            }
        });
        return respond(validationProblem(errors, request));
    }

    @Override
    protected ResponseEntity<Object> handleHttpMessageNotReadable(HttpMessageNotReadableException ex, HttpHeaders headers,
            HttpStatusCode status, WebRequest request) {
        var invalid = messages.getMessage(INVALID_VALUE_KEY, null, ErrorHandlingConfiguration.PT_BR);
        return respond(validationProblem(List.of(new FieldViolation("body", invalid)), request));
    }

    /** Remaining Spring MVC failures (405, 406, 415, unknown route...): same body shape, framework text replaced. */
    @Override
    protected ResponseEntity<Object> handleExceptionInternal(Exception ex, Object body, HttpHeaders headers,
            HttpStatusCode statusCode, WebRequest request) {
        if (body instanceof ProblemDetail pd && pd.getProperties() != null && pd.getProperties().containsKey(CODE)) {
            return super.handleExceptionInternal(ex, body, headers, statusCode, request);
        }
        ErrorCode code = switch (statusCode.value()) {
            case 400 -> ErrorCode.VALIDATION_FAILED;
            case 404 -> ErrorCode.RESOURCE_NOT_FOUND;
            default -> statusCode.is5xxServerError() ? ErrorCode.INTERNAL_ERROR : ErrorCode.REQUEST_NOT_SUPPORTED;
        };
        if (statusCode.is5xxServerError()) {
            log.error("framework failure", ex);
        }
        return super.handleExceptionInternal(ex, problem(code, statusCode, new Object[0], List.of(), request), headers,
            statusCode, request);
    }

    private ResponseEntity<Object> respond(ProblemDetail problem) {
        return ResponseEntity.status(problem.getStatus()).body(problem);
    }

    private ProblemDetail validationProblem(List<FieldViolation> errors, WebRequest request) {
        return problem(ErrorCode.VALIDATION_FAILED, HttpStatus.BAD_REQUEST, new Object[0], errors, request);
    }

    private ProblemDetail problem(ErrorCode code, HttpStatusCode status, Object[] args, List<FieldViolation> errors,
            WebRequest request) {
        var problem = ProblemDetail.forStatusAndDetail(status, messages.getMessage(code.messageKey(), args,
            ErrorHandlingConfiguration.PT_BR));
        problem.setType(URI.create(PROBLEM_TYPE_PREFIX + code.name().toLowerCase(Locale.ROOT).replace('_', '-')));
        problem.setTitle(messages.getMessage("problem.title." + status.value(), null,
            status instanceof HttpStatus http ? http.getReasonPhrase() : "Error", ErrorHandlingConfiguration.PT_BR));
        problem.setProperty(CODE, code.name());
        if (!errors.isEmpty()) {
            problem.setProperty(ERRORS, errors);
        }
        if (request instanceof ServletWebRequest servletRequest) {
            HttpServletRequest http = servletRequest.getRequest();
            problem.setInstance(URI.create(http.getRequestURI().replaceAll("[^A-Za-z0-9/._~-]", "")));
        }
        putIfPresent(problem, "traceId", MDC.get("traceId"));
        putIfPresent(problem, "correlationId", MDC.get(RequestLoggingFilter.MDC_KEY));
        return problem;
    }

    private static void putIfPresent(ProblemDetail problem, String name, String value) {
        if (value != null && !value.isBlank()) {
            problem.setProperty(name, value);
        }
    }

    private static String lastNode(Path path) {
        String name = "";
        for (Path.Node node : path) {
            if (node.getName() != null) {
                name = node.getName();
            }
        }
        return name;
    }

    private static boolean isIntegrityViolation(Throwable ex) {
        for (Throwable t = ex; t != null; t = t.getCause()) {
            if (t instanceof SQLException sql && sql.getSQLState() != null && sql.getSQLState().startsWith("23")) {
                return true;
            }
            for (Class<?> c = t.getClass(); c != null; c = c.getSuperclass()) {
                // By name: spring-tx / Hibernate are not compile dependencies of the shared module.
                if (c.getSimpleName().equals("DataIntegrityViolationException")) {
                    return true;
                }
            }
            if (t.getCause() == t) {
                break;
            }
        }
        return false;
    }

    private static Optional<String> constraintName(Throwable ex) {
        for (Throwable t = ex; t != null; t = t.getCause()) {
            if (t.getMessage() != null) {
                Matcher matcher = CONSTRAINT_NAME.matcher(t.getMessage());
                if (matcher.find()) {
                    return Optional.of(matcher.group(1));
                }
            }
            if (t.getCause() == t) {
                break;
            }
        }
        return Optional.empty();
    }

    /** One entry of {@code errors[]} in the problem body. */
    public record FieldViolation(String field, String message) {
    }
}
