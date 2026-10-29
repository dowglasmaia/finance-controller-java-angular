package com.dowglasmaia.financecontroller.persistence;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.UUID;
import javax.sql.DataSource;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import com.dowglasmaia.financecontroller.support.AbstractPostgresIntegrationTest;

class IdentityMigrationTests extends AbstractPostgresIntegrationTest {
    @Autowired
    JdbcTemplate jdbc;
    @Autowired
    DataSource dataSource;

    @BeforeEach
    void cleanTables() {
        jdbc.update("DELETE FROM refresh_token");
        jdbc.update("DELETE FROM app_user");
    }

    @Test
    void migrationsApplyFromScratchInEmptySchema() {
        var result = Flyway.configure()
            .dataSource(dataSource)
            .schemas("migration_from_scratch")
            .createSchemas(true)
            .locations("classpath:db/migration")
            .load()
            .migrate();
        assertTrue(result.success);
        assertEquals("5", result.targetSchemaVersion);
        List<String> tables = jdbc.queryForList("""
            SELECT table_name FROM information_schema.tables
            WHERE table_schema = 'migration_from_scratch' AND table_name <> 'flyway_schema_history'
            ORDER BY table_name""", String.class);
        assertEquals(List.of("app_user", "budget_category_limit", "budget_month_override", "budget_setting", "category", "money_box", "money_box_entry", "refresh_token", "reserve_goal", "subcategory", "transaction_entry"), tables);
    }

    @Test
    void emailMustBeUnique() {
        insertUser("ana@example.com");
        assertThrows(DataIntegrityViolationException.class, () -> insertUser("ana@example.com"));
    }

    @Test
    void emailMustBeLowercase() {
        assertThrows(DataIntegrityViolationException.class, () -> insertUser("Ana@Example.com"));
    }

    @Test
    void emailUniquenessHoldsRegardlessOfCase() {
        insertUser("ana@example.com");
        // The CHECK rejects the mixed-case spelling, so a case variant can never coexist with the stored one.
        assertThrows(DataIntegrityViolationException.class, () -> insertUser("ANA@example.com"));
        assertEquals(1, jdbc.queryForObject("SELECT count(*) FROM app_user", Integer.class));
    }

    @Test
    void refreshTokenRequiresExistingUser() {
        assertThrows(DataIntegrityViolationException.class, () -> insertToken(UUID.randomUUID()));
    }

    @Test
    void deletingUserCascadesToRefreshTokens() {
        var userId = insertUser("ana@example.com");
        var otherId = insertUser("rui@example.com");
        insertToken(userId);
        insertToken(userId);
        insertToken(otherId);

        jdbc.update("DELETE FROM app_user WHERE id = ?", userId);

        assertEquals(1, jdbc.queryForObject("SELECT count(*) FROM refresh_token", Integer.class));
        assertEquals(otherId, jdbc.queryForObject("SELECT user_id FROM refresh_token", UUID.class));
    }

    @Test
    void lookupIndexesExist() {
        List<String> indexes = jdbc.queryForList("""
            SELECT indexname FROM pg_indexes
            WHERE schemaname = current_schema() AND tablename = 'refresh_token'""", String.class);
        assertTrue(indexes.containsAll(List.of("ix_refresh_token_token_hash", "ix_refresh_token_family_id")));
    }

    private UUID insertUser(String email) {
        var id = UUID.randomUUID();
        jdbc.update("INSERT INTO app_user (id, email, password_hash) VALUES (?, ?, ?)", id, email, "hash");
        return id;
    }

    private void insertToken(UUID userId) {
        jdbc.update("""
            INSERT INTO refresh_token (id, user_id, token_hash, family_id, expires_at)
            VALUES (?, ?, ?, ?, now() + interval '1 day')""",
            UUID.randomUUID(), userId, "token-hash-" + UUID.randomUUID(), UUID.randomUUID());
    }
}
