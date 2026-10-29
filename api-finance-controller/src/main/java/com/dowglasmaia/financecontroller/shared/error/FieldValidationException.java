package com.dowglasmaia.financecontroller.shared.error;

/**
 * Validation failure of one field raised by a business rule ({@code VALIDATION_FAILED}). The message key is resolved
 * in pt-BR by {@link GlobalExceptionHandler} and sent in {@code errors[]}, so the client can show it next to the field.
 */
public class FieldValidationException extends DomainException {
    private final String field;
    private final String messageKey;

    public FieldValidationException(String field, String messageKey) {
        super(ErrorCode.VALIDATION_FAILED);
        this.field = field;
        this.messageKey = messageKey;
    }

    public String field() {
        return field;
    }

    public String messageKey() {
        return messageKey;
    }
}
