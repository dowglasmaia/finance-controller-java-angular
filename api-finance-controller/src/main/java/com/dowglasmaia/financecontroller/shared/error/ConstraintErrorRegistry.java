package com.dowglasmaia.financecontroller.shared.error;

import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

/** Case-insensitive lookup of {@link ConstraintErrorMapping}s. */
public class ConstraintErrorRegistry {
    private final Map<String, ErrorCode> byName = new HashMap<>();

    public ConstraintErrorRegistry(List<ConstraintErrorMapping> mappings) {
        for (var mapping : mappings) {
            if (byName.putIfAbsent(key(mapping.constraintName()), mapping.code()) != null) {
                throw new IllegalStateException("Duplicate constraint mapping: " + mapping.constraintName());
            }
        }
    }

    public Optional<ErrorCode> find(String constraintName) {
        return constraintName == null ? Optional.empty() : Optional.ofNullable(byName.get(key(constraintName)));
    }

    private static String key(String name) {
        return name.toLowerCase(Locale.ROOT);
    }
}
