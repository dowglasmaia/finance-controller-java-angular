package com.dowglasmaia.financecontroller.identity.domain;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashSet;
import java.util.Set;
import org.junit.jupiter.api.Test;

class ResetTokenTests {

    @Test
    void aGeneratedTokenIs256BitsUrlSafeAndItsHashIsSha256Hex() {
        var token = ResetToken.generate();

        assertEquals(43, token.value().length());
        assertTrue(token.value().matches("[A-Za-z0-9_-]+"));
        assertEquals(64, token.hash().length());
        assertTrue(token.hash().matches("[0-9a-f]{64}"));
        assertEquals(token.hash(), ResetToken.hash(token.value()));
        assertNotEquals(token.value(), token.hash());
    }

    @Test
    void tokensDoNotRepeat() {
        Set<String> seen = new HashSet<>();
        for (int i = 0; i < 1000; i++) {
            assertTrue(seen.add(ResetToken.generate().value()));
        }
    }

    @Test
    void knownVectorAndDifferentInputsHaveDifferentHashes() {
        assertEquals("ba7816bf8f01cfea414140de5dae2223b00361a396177a9cb410ff61f20015ad", ResetToken.hash("abc"));
        assertNotEquals(ResetToken.hash("abc"), ResetToken.hash("abd"));
    }

    @Test
    void toStringNeverShowsTheValueOrTheHash() {
        var token = ResetToken.generate();

        assertFalse(token.toString().contains(token.value()));
        assertFalse(token.toString().contains(token.hash()));
    }
}
