package com.dowglasmaia.financecontroller.shared.error;

/**
 * The resource does not exist or belongs to another user; both cases look identical to the client (404, never 403).
 */
public class ResourceNotFoundException extends DomainException {
    public ResourceNotFoundException() {
        super(ErrorCode.RESOURCE_NOT_FOUND);
    }

    /** For a 404 with a more specific catalog code, e.g. {@link ErrorCode#MONEY_BOX_NOT_FOUND}. */
    public ResourceNotFoundException(ErrorCode specificCode) {
        super(specificCode);
    }
}
