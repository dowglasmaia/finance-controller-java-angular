package com.dowglasmaia.financecontroller.transaction.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.dowglasmaia.financecontroller.shared.error.FieldValidationException;
import com.dowglasmaia.financecontroller.shared.money.Currency;
import com.dowglasmaia.financecontroller.transaction.TransactionKind;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class TransactionTests {
    private static final Instant NOW = Instant.parse("2026-10-29T10:00:00Z");
    private static final UUID OWNER = UUID.randomUUID();
    private static final UUID CATEGORY = UUID.randomUUID();

    private static Transaction expense(String amount) {
        return Transaction.create(OWNER, TransactionKind.EXPENSE, "  Mercado  ", amount, Currency.BRL,
            LocalDate.of(2026, 10, 5), CATEGORY, null, "   ", NOW);
    }

    private static void assertField(Runnable action, String field, String messageKey) {
        assertThatThrownBy(action::run).isInstanceOfSatisfying(FieldValidationException.class, e -> {
            assertThat(e.field()).isEqualTo(field);
            assertThat(e.messageKey()).isEqualTo(messageKey);
        });
    }

    @Test
    void createNormalizesTextAndKeepsExactDecimalWithScaleTwo() {
        var t = expense("1234.5");

        assertThat(t.description()).isEqualTo("Mercado");
        assertThat(t.notes()).isNull();
        assertThat(t.amount()).isEqualByComparingTo("1234.50").extracting(BigDecimal::scale).isEqualTo(2);
        assertThat(t.createdAt()).isEqualTo(NOW).isEqualTo(t.updatedAt());
    }

    @Test
    void zeroIsRejectedWithThePositiveMessage() {
        assertField(() -> expense("0"), "amount", "validation.amount.positive");
        assertField(() -> expense("0.00"), "amount", "validation.amount.positive");
    }

    @Test
    void onlyTheApiDecimalFormatIsAccepted() {
        for (String bad : new String[] {"-1", "1.234,56", "1,5", "1e3", "abc", "", " ", "1.234", ".5", "12345678901"}) {
            assertField(() -> expense(bad), "amount", "validation.invalid");
        }
        assertField(() -> expense(null), "amount", "validation.invalid");
    }

    @Test
    void maximumAmountIsAcceptedAndOneCentOverIsNotRepresentable() {
        assertThat(expense("9999999999.99").amount()).isEqualByComparingTo("9999999999.99");
        // 11 dígitos inteiros já falha no formato; o limite exato é garantido também pelo CHECK do banco
        assertField(() -> expense("10000000000.00"), "amount", "validation.invalid");
    }

    @Test
    void descriptionIsRequiredAndLimitedTo120() {
        assertField(() -> Transaction.create(OWNER, TransactionKind.INCOME, "   ", "1.00", Currency.BRL,
            LocalDate.of(2026, 1, 1), CATEGORY, null, null, NOW), "description", "validation.required");
        assertField(() -> Transaction.create(OWNER, TransactionKind.INCOME, "x".repeat(121), "1.00", Currency.BRL,
            LocalDate.of(2026, 1, 1), CATEGORY, null, null, NOW), "description", "validation.description.length");
        assertThat(Transaction.create(OWNER, TransactionKind.INCOME, "x".repeat(120), "1.00", Currency.BRL,
            LocalDate.of(2026, 1, 1), CATEGORY, null, null, NOW).description()).hasSize(120);
    }

    @Test
    void dateRangeAllowsFutureAndRejectsOutsideTwoThousandToTwentyOneHundred() {
        for (LocalDate ok : new LocalDate[] {LocalDate.of(2000, 1, 1), LocalDate.of(2100, 12, 31), LocalDate.of(2099, 1, 1)}) {
            assertThat(Transaction.create(OWNER, TransactionKind.INCOME, "a", "1.00", Currency.EUR, ok, CATEGORY, null,
                null, NOW).date()).isEqualTo(ok);
        }
        for (LocalDate bad : new LocalDate[] {LocalDate.of(1999, 12, 31), LocalDate.of(2101, 1, 1)}) {
            assertField(() -> Transaction.create(OWNER, TransactionKind.INCOME, "a", "1.00", Currency.EUR, bad,
                CATEGORY, null, null, NOW), "date", "validation.date.range");
        }
    }

    @Test
    void updatePreservesIdOwnerAndCreatedAtAndBumpsUpdatedAt() {
        var original = expense("10.00");
        var later = NOW.plusSeconds(60);

        var edited = original.update(TransactionKind.INCOME, "Salário", "20.00", Currency.EUR,
            LocalDate.of(2026, 11, 1), UUID.randomUUID(), null, "obs", later);

        assertThat(edited.id()).isEqualTo(original.id());
        assertThat(edited.ownerId()).isEqualTo(original.ownerId());
        assertThat(edited.createdAt()).isEqualTo(NOW);
        assertThat(edited.updatedAt()).isEqualTo(later);
        assertThat(edited.kind()).isEqualTo(TransactionKind.INCOME);
        assertThat(edited.notes()).isEqualTo("obs");
    }

    @Test
    void notesLongerThanFiveHundredAreRejected() {
        assertField(() -> Transaction.create(OWNER, TransactionKind.INCOME, "a", "1.00", Currency.BRL,
            LocalDate.of(2026, 1, 1), CATEGORY, null, "n".repeat(501), NOW), "notes", "validation.notes.length");
    }

    @Test
    void totalsBalanceIsDerivedAndFilterZeroesTheOtherDirection() {
        var totals = new Totals(new BigDecimal("1000.00"), new BigDecimal("250.50"));

        assertThat(totals.balance()).isEqualByComparingTo("749.50");
        assertThat(totals.only(TransactionKind.INCOME).balance()).isEqualByComparingTo("1000.00");
        assertThat(totals.only(TransactionKind.EXPENSE).balance()).isEqualByComparingTo("-250.50");
        assertThat(totals.only(null)).isEqualTo(totals);
    }
}
