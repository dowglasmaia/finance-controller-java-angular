package com.dowglasmaia.financecontroller.identity.infrastructure.persistence;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.dowglasmaia.financecontroller.identity.application.port.out.RefreshSessionRepository;
import com.dowglasmaia.financecontroller.identity.application.port.out.UserRepository;
import com.dowglasmaia.financecontroller.identity.domain.Email;
import com.dowglasmaia.financecontroller.identity.domain.RefreshSession;
import com.dowglasmaia.financecontroller.identity.domain.User;
import com.dowglasmaia.financecontroller.identity.domain.UserId;
import com.dowglasmaia.financecontroller.support.AbstractPostgresIntegrationTest;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;

class IdentityPersistenceIntegrationTests extends AbstractPostgresIntegrationTest {

    // timestamptz guarda microssegundos
    private static final Instant NOW = Instant.now().truncatedTo(ChronoUnit.MICROS);

    @Autowired
    UserRepository users;
    @Autowired
    RefreshSessionRepository sessions;
    @Autowired
    JdbcTemplate jdbc;

    @BeforeEach
    void cleanTables() {
        jdbc.update("DELETE FROM refresh_token");
        jdbc.update("DELETE FROM app_user");
    }

    @Test
    void savesAndFindsUserByEmailAndId() {
        var saved = users.save(newUser("Ana@Example.com"));

        assertEquals("ana@example.com", jdbc.queryForObject("SELECT email FROM app_user", String.class));
        assertEquals(saved, users.findByEmail(Email.of("ANA@example.com")).orElseThrow());
        assertEquals(saved, users.findById(saved.id()).orElseThrow());
    }

    @Test
    void returnsEmptyForUnknownUser() {
        assertTrue(users.findByEmail(Email.of("nobody@example.com")).isEmpty());
        assertTrue(users.findById(UserId.generate()).isEmpty());
    }

    @Test
    void existsByEmailReflectsStoredUsers() {
        assertFalse(users.existsByEmail(Email.of("ana@example.com")));
        users.save(newUser("ana@example.com"));
        assertTrue(users.existsByEmail(Email.of("ANA@example.com")));
    }

    @Test
    void duplicateEmailViolatesUniqueConstraintImmediately() {
        users.save(newUser("ana@example.com"));

        var ex = assertThrows(DataIntegrityViolationException.class, () -> users.save(newUser("ana@example.com")));

        assertTrue(String.valueOf(ex.getMessage()).contains("uq_app_user_email"), ex.getMessage());
        assertEquals(1, jdbc.queryForObject("SELECT count(*) FROM app_user", Integer.class));
    }

    @Test
    void savesAndFindsRefreshSessionByHash() {
        var user = users.save(newUser("ana@example.com"));
        var session = RefreshSession.start(user.id(), "hash-1", UUID.randomUUID(), NOW, NOW.plus(7, ChronoUnit.DAYS));

        sessions.save(session);

        var found = sessions.findByTokenHash("hash-1").orElseThrow();
        assertEquals(session, found);
        assertNull(found.revokedAt());
        assertTrue(sessions.findByTokenHash("other").isEmpty());
    }

    @Test
    void revokeAffectsOnlyTargetSessionAndKeepsFirstRevocation() {
        var user = users.save(newUser("ana@example.com"));
        var family = UUID.randomUUID();
        var target = sessions.save(session(user, "h1", family));
        sessions.save(session(user, "h2", family));

        var first = NOW.plusSeconds(10);
        sessions.revoke(target.id(), first);
        sessions.revoke(target.id(), first.plusSeconds(60));

        assertEquals(first, sessions.findByTokenHash("h1").orElseThrow().revokedAt());
        assertNull(sessions.findByTokenHash("h2").orElseThrow().revokedAt());
    }

    @Test
    void revokeFamilyAffectsOnlyUnrevokedSessionsOfThatFamily() {
        var user = users.save(newUser("ana@example.com"));
        var family = UUID.randomUUID();
        var alreadyRevoked = sessions.save(session(user, "h1", family));
        sessions.save(session(user, "h2", family));
        sessions.save(session(user, "h3", UUID.randomUUID()));

        var earlier = NOW.plusSeconds(5);
        var later = NOW.plusSeconds(30);
        sessions.revoke(alreadyRevoked.id(), earlier);
        sessions.revokeFamily(family, later);

        assertEquals(earlier, sessions.findByTokenHash("h1").orElseThrow().revokedAt());
        assertEquals(later, sessions.findByTokenHash("h2").orElseThrow().revokedAt());
        assertNull(sessions.findByTokenHash("h3").orElseThrow().revokedAt());
    }

    private static User newUser(String rawEmail) {
        return new User(UserId.generate(), Email.of(rawEmail), "bcrypt-hash", NOW);
    }

    private static RefreshSession session(User user, String hash, UUID family) {
        return RefreshSession.start(user.id(), hash, family, NOW, NOW.plus(7, ChronoUnit.DAYS));
    }
}
