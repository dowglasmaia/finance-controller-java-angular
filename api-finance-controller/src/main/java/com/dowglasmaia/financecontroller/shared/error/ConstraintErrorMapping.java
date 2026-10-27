package com.dowglasmaia.financecontroller.shared.error;

/**
 * Maps a database constraint name (unique, foreign key) to the catalog code it means. Each module declares its own
 * mappings as beans, e.g. {@code new ConstraintErrorMapping("uk_category_owner_name", ErrorCode.CATEGORY_NAME_DUPLICATED)}.
 */
public record ConstraintErrorMapping(String constraintName, ErrorCode code) {
}
