package com.dowglasmaia.financecontroller.budget.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.dowglasmaia.financecontroller.shared.error.FieldValidationException;
import com.dowglasmaia.financecontroller.shared.money.Currency;
import java.math.BigDecimal;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class BudgetDomainTests {

    @ParameterizedTest
    @CsvSource({
        "0, 1000, OK", "700, 1000, OK", "799.99, 1000, OK", "800, 1000, WARNING", "950, 1000, WARNING",
        "999.99, 1000, WARNING", "1000, 1000, EXCEEDED", "1200, 1000, EXCEEDED", "0, 0, NONE", "500, 0, NONE",
        "10, -5, NONE"
    })
    void statusTableIsExact(String spent, String limit, BudgetStatus expected) {
        assertThat(BudgetStatus.of(new BigDecimal(spent), new BigDecimal(limit))).isEqualTo(expected);
    }

    @Test
    void noLimitIsNone() {
        assertThat(BudgetStatus.of(new BigDecimal("50"), null)).isEqualTo(BudgetStatus.NONE);
    }

    @Test
    void thresholdIsComparedWithoutFloatingPointError() {
        // 0.8 * 1000.10 = 800.08 exato; 800.07 ainda não é WARNING
        assertThat(BudgetStatus.of(new BigDecimal("800.07"), new BigDecimal("1000.10"))).isEqualTo(BudgetStatus.OK);
        assertThat(BudgetStatus.of(new BigDecimal("800.08"), new BigDecimal("1000.10"))).isEqualTo(BudgetStatus.WARNING);
    }

    @Test
    void worseThanFollowsSeverity() {
        assertThat(BudgetStatus.WARNING.worseThan(BudgetStatus.OK)).isTrue();
        assertThat(BudgetStatus.EXCEEDED.worseThan(BudgetStatus.WARNING)).isTrue();
        assertThat(BudgetStatus.EXCEEDED.worseThan(BudgetStatus.EXCEEDED)).isFalse();
        assertThat(BudgetStatus.OK.worseThan(BudgetStatus.EXCEEDED)).isFalse();
    }

    @Test
    void progressHasFractionWithFourDecimalsAndRemaining() {
        var quarter = Progress.of(null, new BigDecimal("200.00"), new BigDecimal("50.00"));
        assertThat(quarter.fraction()).isEqualByComparingTo("0.2500");
        assertThat(quarter.fraction().scale()).isEqualTo(4);
        assertThat(quarter.remaining()).isEqualByComparingTo("150.00");

        var over = Progress.of(null, new BigDecimal("200.00"), new BigDecimal("250.00"));
        assertThat(over.remaining()).isEqualByComparingTo("-50.00");
        assertThat(over.fraction()).isEqualByComparingTo("1.2500");
        assertThat(over.status()).isEqualTo(BudgetStatus.EXCEEDED);

        assertThat(Progress.of(null, new BigDecimal("3.00"), new BigDecimal("1.00")).fraction()).isEqualByComparingTo("0.3333");
        assertThat(Progress.of(null, new BigDecimal("3.00"), new BigDecimal("2.00")).fraction()).isEqualByComparingTo("0.6667");
    }

    @Test
    void progressWithoutLimitHasNoFractionNorRemaining() {
        var none = Progress.of(UUID.randomUUID(), null, new BigDecimal("80.00"));

        assertThat(none.status()).isEqualTo(BudgetStatus.NONE);
        assertThat(none.limit()).isNull();
        assertThat(none.fraction()).isNull();
        assertThat(none.remaining()).isNull();
        assertThat(none.spent()).isEqualByComparingTo("80.00");
    }

    @Test
    void effectiveLimitFollowsOverrideThenRecurring() {
        var recurring = new BigDecimal("1000.00");
        var withOverride = new BudgetConfig(Currency.BRL, recurring, MonthOverride.ofLimit(new BigDecimal("1500.00")), Map.of());
        var disabled = new BudgetConfig(Currency.BRL, recurring, MonthOverride.off(), Map.of());
        var plain = new BudgetConfig(Currency.BRL, recurring, null, Map.of());
        var none = new BudgetConfig(Currency.BRL, null, null, Map.of());
        var onlyOverride = new BudgetConfig(Currency.BRL, null, MonthOverride.ofLimit(new BigDecimal("300.00")), Map.of());

        assertThat(withOverride.effectiveLimit()).isEqualByComparingTo("1500.00");
        assertThat(disabled.effectiveLimit()).isNull();
        assertThat(plain.effectiveLimit()).isEqualByComparingTo("1000.00");
        assertThat(none.effectiveLimit()).isNull();
        assertThat(onlyOverride.effectiveLimit()).isEqualByComparingTo("300.00");
    }

    @Test
    void limitAmountAcceptsOnlyTheApiFormatAndTheMaximum() {
        assertThat(LimitAmount.parse("1000", "f")).isEqualByComparingTo("1000.00");
        assertThat(LimitAmount.parse("0", "f")).isEqualByComparingTo("0.00");
        assertThat(LimitAmount.parse(null, "f")).isNull();
        assertThat(LimitAmount.parse("  ", "f")).isNull();
        assertThat(LimitAmount.positiveOrNull(new BigDecimal("0.00"))).isNull();
        assertThat(LimitAmount.positiveOrNull(new BigDecimal("0.01"))).isNotNull();
        for (String bad : new String[] {"-1", "1,5", "abc", "1.234", "12345678901"}) {
            assertThatThrownBy(() -> LimitAmount.parse(bad, "f")).isInstanceOf(FieldValidationException.class);
        }
        assertThat(LimitAmount.parse("9999999999.99", "f")).isEqualByComparingTo(LimitAmount.MAX);
    }
}
