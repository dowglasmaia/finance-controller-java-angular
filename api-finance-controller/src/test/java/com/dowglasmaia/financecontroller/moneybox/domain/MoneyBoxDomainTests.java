package com.dowglasmaia.financecontroller.moneybox.domain;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.dowglasmaia.financecontroller.shared.error.DomainException;
import com.dowglasmaia.financecontroller.shared.error.ErrorCode;
import com.dowglasmaia.financecontroller.shared.error.FieldValidationException;
import com.dowglasmaia.financecontroller.shared.money.Currency;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class MoneyBoxDomainTests {
    private static final UUID OWNER = UUID.randomUUID();
    private static final UUID BOX = UUID.randomUUID();
    private static final Instant T0 = Instant.parse("2026-10-01T10:00:00Z");

    private static MoneyBoxEntry entry(EntryKind kind, String amount, String date, int secondsAfter) {
        return MoneyBoxEntry.create(OWNER, BOX, kind, amount, LocalDate.parse(date), null, T0.plusSeconds(secondsAfter));
    }

    @Test
    void balanceIsDepositsMinusWithdrawals() {
        var entries = List.of(entry(EntryKind.DEPOSIT, "1000.00", "2026-10-01", 0),
            entry(EntryKind.DEPOSIT, "500.00", "2026-10-02", 1), entry(EntryKind.WITHDRAWAL, "300.00", "2026-10-03", 2));

        assertEquals(new BigDecimal("1200.00"), Ledger.balance(entries));
        assertTrue(Ledger.neverNegative(entries));
    }

    @Test
    void withdrawalEqualToBalanceIsAllowedAndLeavesZero() {
        var entries = List.of(entry(EntryKind.DEPOSIT, "100.00", "2026-10-01", 0),
            entry(EntryKind.WITHDRAWAL, "100.00", "2026-10-02", 1));

        assertTrue(Ledger.neverNegative(entries));
        assertEquals(new BigDecimal("0.00"), Ledger.balance(entries));
    }

    @Test
    void withdrawalAboveBalanceOrWithZeroBalanceIsInsufficient() {
        var above = List.of(entry(EntryKind.DEPOSIT, "100.00", "2026-10-01", 0),
            entry(EntryKind.WITHDRAWAL, "100.01", "2026-10-02", 1));
        var empty = List.of(entry(EntryKind.WITHDRAWAL, "0.01", "2026-10-02", 0));

        assertEquals(ErrorCode.INSUFFICIENT_BALANCE, assertThrows(DomainException.class, () -> Ledger.requireNeverNegative(above)).code());
        assertEquals(ErrorCode.INSUFFICIENT_BALANCE, assertThrows(DomainException.class, () -> Ledger.requireNeverNegative(empty)).code());
    }

    @Test
    void negativeAtAnyPointOfTheHistoryIsRefusedEvenIfTheFinalBalanceIsPositive() {
        // a retirada em 02/10 vem antes do depósito de 05/10: no meio do histórico o saldo seria -50
        var entries = List.of(entry(EntryKind.WITHDRAWAL, "50.00", "2026-10-02", 0),
            entry(EntryKind.DEPOSIT, "200.00", "2026-10-05", 1));

        assertFalse(Ledger.neverNegative(entries));
    }

    @Test
    void sameDayEntriesFollowCreationOrder() {
        var depositFirst = List.of(entry(EntryKind.DEPOSIT, "10.00", "2026-10-02", 0),
            entry(EntryKind.WITHDRAWAL, "10.00", "2026-10-02", 1));
        var withdrawalFirst = List.of(entry(EntryKind.WITHDRAWAL, "10.00", "2026-10-02", 0),
            entry(EntryKind.DEPOSIT, "10.00", "2026-10-02", 1));

        assertTrue(Ledger.neverNegative(depositFirst));
        assertFalse(Ledger.neverNegative(withdrawalFirst));
    }

    @Test
    void ratioIsClampedBetweenZeroAndOneWithFourDecimals() {
        assertEquals(new BigDecimal("0.3333"), Amounts.ratio(new BigDecimal("100.00"), new BigDecimal("300.00")));
        assertEquals(new BigDecimal("1.0000"), Amounts.ratio(new BigDecimal("500.00"), new BigDecimal("300.00")));
        assertEquals(new BigDecimal("0.0000"), Amounts.ratio(new BigDecimal("100.00"), null));
        assertEquals(new BigDecimal("0.0000"), Amounts.ratio(new BigDecimal("0.00"), new BigDecimal("300.00")));
    }

    @Test
    void amountsAcceptOnlyTheApiFormat() {
        assertEquals(new BigDecimal("12.50"), Amounts.parse("12.5", "amount"));
        assertNull(Amounts.parse(" ", "amount"));
        for (String bad : List.of("-1", "1,5", "1e3", "1.234", "12345678901", "abc")) {
            assertThrows(FieldValidationException.class, () -> Amounts.parse(bad, "amount"), bad);
        }
    }

    @Test
    void entryRequiresPositiveAmountAndValidDate() {
        assertThrows(FieldValidationException.class, () -> entry(EntryKind.DEPOSIT, "0", "2026-10-01", 0));
        assertThrows(FieldValidationException.class, () -> entry(EntryKind.DEPOSIT, "10", "1999-12-31", 0));
        assertThrows(FieldValidationException.class, () -> entry(EntryKind.DEPOSIT, "10", "2101-01-01", 0));
    }

    @Test
    void goalZeroOrAbsentMeansNoGoalAndValidationApplies() {
        var none = MoneyBox.create(OWNER, " Viagem ", null, "plane", "#112233", Currency.BRL, "0", T0);
        var some = MoneyBox.create(OWNER, "Carro", "Na poupança", "car", "#112233", Currency.EUR, "1500", T0);

        assertEquals("Viagem", none.name());
        assertNull(none.goal());
        assertEquals(new BigDecimal("1500.00"), some.goal());
        assertThrows(FieldValidationException.class, () -> MoneyBox.create(OWNER, "", null, "x", "#112233", Currency.BRL, null, T0));
        assertThrows(FieldValidationException.class, () -> MoneyBox.create(OWNER, "A", null, "x", "112233", Currency.BRL, null, T0));
        assertThrows(FieldValidationException.class, () -> MoneyBox.create(OWNER, "A", null, "x", "#112233", Currency.BRL, "-5", T0));
    }
}
