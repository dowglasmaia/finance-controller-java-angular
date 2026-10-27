package com.dowglasmaia.financecontroller.shared.error;

/**
 * Typed business failure. Carries only the catalog code and the message arguments (e.g. currencies); the
 * user-facing text is resolved by {@link GlobalExceptionHandler}, so nothing internal can leak into a response.
 */
public class DomainException extends RuntimeException {
    private final ErrorCode code;
    private final Object[] args;

    public DomainException(ErrorCode code, Object... args) {
        super(code.name());
        this.code = code;
        this.args = args.clone();
    }

    public ErrorCode code() {
        return code;
    }

    public Object[] args() {
        return args.clone();
    }
}
