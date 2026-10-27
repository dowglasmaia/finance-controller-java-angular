package com.dowglasmaia.financecontroller.shared.error;

import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Valid;
import jakarta.validation.Validator;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.math.BigDecimal;
import java.sql.SQLException;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** Provokes every failure path of {@link GlobalExceptionHandler}; lives in test sources only. */
@RestController
class ErrorProbeController {
    record Payload(
        @NotBlank(message = "{validation.required}") String name,
        @NotNull(message = "{validation.amount.required}") @Positive(message = "{validation.amount.positive}") BigDecimal amount) {
    }

    private final Validator validator;

    ErrorProbeController(Validator validator) {
        this.validator = validator;
    }

    @GetMapping("/probe/domain/{code}")
    String domain(@PathVariable ErrorCode code) {
        throw switch (code) {
            case CURRENCY_UNSUPPORTED -> new DomainException(code, "XYZ");
            case CURRENCY_MISMATCH -> new DomainException(code, "EUR", "BRL");
            default -> new DomainException(code);
        };
    }

    @GetMapping("/probe/foreign-resource")
    String foreignResource() {
        throw new ResourceNotFoundException();
    }

    @GetMapping("/probe/unique")
    String unique() {
        throw new RuntimeException("could not execute statement", new SQLException(
            "ERROR: duplicate key value violates unique constraint \"uk_test_name\"\n  Detail: Key (name)=(secret) already exists.",
            "23505"));
    }

    @GetMapping("/probe/foreign-key")
    String foreignKey() {
        throw new RuntimeException("could not execute statement", new SQLException(
            "ERROR: update or delete on table \"category\" violates foreign key constraint \"fk_test_category\" on table \"txn\"",
            "23503"));
    }

    @GetMapping("/probe/unmapped-integrity")
    String unmappedConstraint() {
        throw new RuntimeException("could not execute statement", new SQLException(
            "ERROR: new row violates check constraint \"ck_secret_internal\"", "23514"));
    }

    @GetMapping("/probe/boom")
    String boom() {
        throw new IllegalStateException("SELECT * FROM users WHERE password='secret' at org.hibernate.Internal");
    }

    @PostMapping("/probe/body")
    String body(@Valid @RequestBody Payload payload) {
        return payload.name();
    }

    @GetMapping("/probe/param")
    String param(@RequestParam("amount") @Positive(message = "{validation.amount.positive}") BigDecimal amount) {
        return amount.toPlainString();
    }

    @GetMapping("/probe/constraint-violation")
    String constraintViolation() {
        throw new ConstraintViolationException(validator.validate(new Payload(" ", BigDecimal.ONE)));
    }
}
