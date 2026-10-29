package com.dowglasmaia.financecontroller.operations;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.dowglasmaia.financecontroller.category.CategoryType;
import com.dowglasmaia.financecontroller.category.domain.Category;
import com.dowglasmaia.financecontroller.moneybox.domain.EntryKind;
import com.dowglasmaia.financecontroller.moneybox.domain.MoneyBox;
import com.dowglasmaia.financecontroller.moneybox.domain.MoneyBoxEntry;
import com.dowglasmaia.financecontroller.preferences.domain.Preference;
import com.dowglasmaia.financecontroller.preferences.domain.ThemeMode;
import com.dowglasmaia.financecontroller.shared.money.Currency;
import com.dowglasmaia.financecontroller.transaction.TransactionKind;
import com.dowglasmaia.financecontroller.transaction.domain.Transaction;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;

/**
 * Every guard of the financial domain, hit from the outside: invalid input never builds an object (QUALITY-SPEC-01).
 * These are the validation branches the flow tests do not reach one by one.
 */
class DomainGuardTests {
    private static final UUID OWNER = UUID.randomUUID();
    private static final UUID ID = UUID.randomUUID();
    private static final Instant NOW = Instant.parse("2026-10-01T10:00:00Z");
    private static final LocalDate DAY = LocalDate.parse("2026-10-01");

    private static void rejected(Executable build) {
        assertThrows(RuntimeException.class, build);
    }

    @Test
    void categoryRejectsEveryInvalidField() {
        rejected(() -> Category.create(OWNER, null, CategoryType.EXPENSE, "x", "#112233", null));
        rejected(() -> Category.create(OWNER, "  ", CategoryType.EXPENSE, "x", "#112233", null));
        rejected(() -> Category.create(OWNER, "n".repeat(61), CategoryType.EXPENSE, "x", "#112233", null));
        rejected(() -> Category.create(OWNER, "A", null, "x", "#112233", null));
        rejected(() -> Category.create(OWNER, "A", CategoryType.EXPENSE, null, "#112233", null));
        rejected(() -> Category.create(OWNER, "A", CategoryType.EXPENSE, " ", "#112233", null));
        rejected(() -> Category.create(OWNER, "A", CategoryType.EXPENSE, "i".repeat(41), "#112233", null));
        rejected(() -> Category.create(OWNER, "A", CategoryType.EXPENSE, "x", null, null));
        rejected(() -> Category.create(OWNER, "A", CategoryType.EXPENSE, "x", "112233", null));
        rejected(() -> Category.create(null, "A", CategoryType.EXPENSE, "x", "#112233", null));
        assertDoesNotThrow(() -> Category.create(OWNER, "  A  ", CategoryType.BOTH, "x", "#aabbcc", List.of("Um", "um", "OUTROS")));
    }

    @Test
    void transactionRejectsEveryInvalidField() {
        UUID category = UUID.randomUUID();
        rejected(() -> Transaction.create(OWNER, TransactionKind.EXPENSE, null, "1.00", Currency.BRL, DAY, category, null, null, NOW));
        rejected(() -> Transaction.create(OWNER, TransactionKind.EXPENSE, " ", "1.00", Currency.BRL, DAY, category, null, null, NOW));
        rejected(() -> Transaction.create(OWNER, TransactionKind.EXPENSE, "d".repeat(121), "1.00", Currency.BRL, DAY, category, null, null, NOW));
        for (String amount : new String[] {null, "", "0", "0.00", "abc", "-1", "1.234", "99999999999", "1e3"}) {
            rejected(() -> Transaction.create(OWNER, TransactionKind.EXPENSE, "d", amount, Currency.BRL, DAY, category, null, null, NOW));
        }
        for (LocalDate date : new LocalDate[] {null, LocalDate.parse("1999-12-31"), LocalDate.parse("2101-01-01")}) {
            rejected(() -> Transaction.create(OWNER, TransactionKind.EXPENSE, "d", "1.00", Currency.BRL, date, category, null, null, NOW));
        }
        rejected(() -> Transaction.create(OWNER, TransactionKind.EXPENSE, "d", "1.00", Currency.BRL, DAY, category, null, "n".repeat(501), NOW));
        rejected(() -> Transaction.create(OWNER, null, "d", "1.00", Currency.BRL, DAY, category, null, null, NOW));
        rejected(() -> Transaction.create(OWNER, TransactionKind.EXPENSE, "d", "1.00", null, DAY, category, null, null, NOW));
        rejected(() -> Transaction.create(OWNER, TransactionKind.EXPENSE, "d", "1.00", Currency.BRL, DAY, null, null, null, NOW));
        rejected(() -> Transaction.create(null, TransactionKind.EXPENSE, "d", "1.00", Currency.BRL, DAY, category, null, null, NOW));
        rejected(() -> Transaction.create(OWNER, TransactionKind.EXPENSE, "d", "1.00", Currency.BRL, DAY, category, null, null, null));
        for (BigDecimal amount : new BigDecimal[] {null, BigDecimal.ZERO, new BigDecimal("-1"), new BigDecimal("1.234"),
            new BigDecimal("10000000000.00")}) {
            rejected(() -> new Transaction(ID, OWNER, TransactionKind.EXPENSE, "d", amount, Currency.BRL, DAY, category, null, null, NOW, NOW));
        }
        assertDoesNotThrow(() -> Transaction.create(OWNER, TransactionKind.INCOME, "  d  ", "9999999999.99", Currency.EUR, DAY, category, null, "  ", NOW));
    }

    @Test
    void moneyBoxRejectsEveryInvalidField() {
        rejected(() -> MoneyBox.create(OWNER, null, null, "x", "#112233", Currency.BRL, null, NOW));
        rejected(() -> MoneyBox.create(OWNER, " ", null, "x", "#112233", Currency.BRL, null, NOW));
        rejected(() -> MoneyBox.create(OWNER, "n".repeat(101), null, "x", "#112233", Currency.BRL, null, NOW));
        rejected(() -> MoneyBox.create(OWNER, "A", "d".repeat(256), "x", "#112233", Currency.BRL, null, NOW));
        rejected(() -> MoneyBox.create(OWNER, "A", null, null, "#112233", Currency.BRL, null, NOW));
        rejected(() -> MoneyBox.create(OWNER, "A", null, "i".repeat(41), "#112233", Currency.BRL, null, NOW));
        rejected(() -> MoneyBox.create(OWNER, "A", null, "x", null, Currency.BRL, null, NOW));
        rejected(() -> MoneyBox.create(OWNER, "A", null, "x", "#12", Currency.BRL, null, NOW));
        rejected(() -> MoneyBox.create(OWNER, "A", null, "x", "#112233", null, null, NOW));
        rejected(() -> MoneyBox.create(null, "A", null, "x", "#112233", Currency.BRL, null, NOW));
        rejected(() -> MoneyBox.create(OWNER, "A", null, "x", "#112233", Currency.BRL, null, null));
        rejected(() -> MoneyBox.create(OWNER, "A", null, "x", "#112233", Currency.BRL, "-1", NOW));
        rejected(() -> MoneyBox.create(OWNER, "A", null, "x", "#112233", Currency.BRL, "abc", NOW));
        rejected(() -> new MoneyBox(ID, OWNER, "A", null, "x", "#112233", Currency.BRL, new BigDecimal("-1"), NOW));
        rejected(() -> new MoneyBox(ID, OWNER, "A", null, "x", "#112233", Currency.BRL, new BigDecimal("10000000000"), NOW));
        var box = MoneyBox.create(OWNER, "A", "  ", "x", "#112233", Currency.BRL, "0", NOW);
        assertNull(box.description());
        assertNull(box.goal());
        assertDoesNotThrow(() -> box.update("B", "onde", "y", "#ffffff", Currency.EUR, "10.5"));
    }

    @Test
    void moneyBoxEntryRejectsEveryInvalidField() {
        UUID boxId = UUID.randomUUID();
        for (BigDecimal amount : new BigDecimal[] {null, BigDecimal.ZERO, new BigDecimal("-5"), new BigDecimal("1.234"),
            new BigDecimal("10000000000.00")}) {
            rejected(() -> new MoneyBoxEntry(ID, OWNER, boxId, EntryKind.DEPOSIT, amount, DAY, null, NOW));
        }
        rejected(() -> new MoneyBoxEntry(ID, OWNER, boxId, null, BigDecimal.TEN, DAY, null, NOW));
        for (LocalDate date : new LocalDate[] {null, LocalDate.parse("1999-12-31"), LocalDate.parse("2101-01-01")}) {
            rejected(() -> new MoneyBoxEntry(ID, OWNER, boxId, EntryKind.DEPOSIT, BigDecimal.TEN, date, null, NOW));
        }
        rejected(() -> new MoneyBoxEntry(null, OWNER, boxId, EntryKind.DEPOSIT, BigDecimal.TEN, DAY, null, NOW));
        rejected(() -> new MoneyBoxEntry(ID, null, boxId, EntryKind.DEPOSIT, BigDecimal.TEN, DAY, null, NOW));
        rejected(() -> new MoneyBoxEntry(ID, OWNER, null, EntryKind.DEPOSIT, BigDecimal.TEN, DAY, null, NOW));
        rejected(() -> new MoneyBoxEntry(ID, OWNER, boxId, EntryKind.DEPOSIT, BigDecimal.TEN, DAY, null, null));
        rejected(() -> new MoneyBoxEntry(ID, OWNER, boxId, EntryKind.DEPOSIT, BigDecimal.TEN, DAY, "n".repeat(256), NOW));
        rejected(() -> MoneyBoxEntry.create(OWNER, boxId, EntryKind.DEPOSIT, null, DAY, null, NOW));
        rejected(() -> MoneyBoxEntry.create(OWNER, boxId, EntryKind.DEPOSIT, " ", DAY, null, NOW));
        rejected(() -> MoneyBoxEntry.create(OWNER, boxId, EntryKind.DEPOSIT, "1,5", DAY, null, NOW));
        var entry = MoneyBoxEntry.create(OWNER, boxId, EntryKind.WITHDRAWAL, "5", DAY, "   ", NOW);
        assertNull(entry.note());
        assertDoesNotThrow(() -> entry.update(EntryKind.DEPOSIT, "7.50", DAY, "ok"));
    }

    @Test
    void preferenceRejectsMissingParts() {
        rejected(() -> new Preference(null, Currency.BRL, ThemeMode.DARK));
        rejected(() -> new Preference(OWNER, null, ThemeMode.DARK));
        rejected(() -> new Preference(OWNER, Currency.BRL, null));
        assertDoesNotThrow(() -> Preference.defaults(OWNER));
    }
}
