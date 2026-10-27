package com.dowglasmaia.financecontroller.shared.error;

import org.springframework.http.HttpStatus;

/**
 * Stable error catalog (BACKEND-SPEC-04). The {@code code} sent to clients is the constant name; the pt-BR text lives
 * in {@code messages_pt_BR.properties} under {@code error.<CODE>}.
 */
public enum ErrorCode {
    VALIDATION_FAILED(HttpStatus.BAD_REQUEST),
    RESOURCE_NOT_FOUND(HttpStatus.NOT_FOUND),
    CATEGORY_NAME_DUPLICATED(HttpStatus.CONFLICT),
    CATEGORY_IN_USE(HttpStatus.CONFLICT),
    CATEGORY_TYPE_MISMATCH(HttpStatus.CONFLICT),
    SUBCATEGORY_PROTECTED(HttpStatus.CONFLICT),
    SUBCATEGORY_NAME_DUPLICATED(HttpStatus.CONFLICT),
    SUBCATEGORY_NOT_IN_CATEGORY(HttpStatus.CONFLICT),
    CATEGORY_TYPE_CONFLICT(HttpStatus.CONFLICT),
    UNAUTHENTICATED(HttpStatus.UNAUTHORIZED),
    INVALID_CREDENTIALS(HttpStatus.UNAUTHORIZED),
    EMAIL_ALREADY_REGISTERED(HttpStatus.CONFLICT),
    TOO_MANY_ATTEMPTS(HttpStatus.TOO_MANY_REQUESTS),
    PASSWORD_RESET_TOKEN_INVALID(HttpStatus.BAD_REQUEST),
    CURRENCY_UNSUPPORTED(HttpStatus.BAD_REQUEST),
    CURRENCY_MISMATCH(HttpStatus.CONFLICT),
    CURRENCY_LOCKED(HttpStatus.CONFLICT),
    MONEY_BOX_NOT_FOUND(HttpStatus.NOT_FOUND),
    INSUFFICIENT_BALANCE(HttpStatus.CONFLICT),
    INTERNAL_ERROR(HttpStatus.INTERNAL_SERVER_ERROR),
    /** Not in the spec catalog: framework-level rejections (405, 406, 415, ...) that keep their own HTTP status. */
    REQUEST_NOT_SUPPORTED(HttpStatus.BAD_REQUEST);

    private final HttpStatus status;

    ErrorCode(HttpStatus status) {
        this.status = status;
    }

    public HttpStatus status() {
        return status;
    }

    public String messageKey() {
        return "error." + name();
    }
}
