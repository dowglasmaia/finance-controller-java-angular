package com.dowglasmaia.financecontroller.identity.domain;

import java.time.Instant;
import java.util.Objects;

public record User(UserId id, Email email, String passwordHash, Instant createdAt) {

    public User {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(email, "email");
        Objects.requireNonNull(passwordHash, "passwordHash");
        Objects.requireNonNull(createdAt, "createdAt");
    }
}
