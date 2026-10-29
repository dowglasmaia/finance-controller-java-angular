package com.dowglasmaia.financecontroller.budget.application;

import static org.assertj.core.api.Assertions.assertThat;

import com.dowglasmaia.financecontroller.budget.application.port.out.BudgetRepository;
import com.dowglasmaia.financecontroller.budget.domain.BudgetConfig;
import com.dowglasmaia.financecontroller.budget.domain.MonthOverride;
import com.dowglasmaia.financecontroller.shared.money.Currency;
import com.dowglasmaia.financecontroller.transaction.BudgetAlert;
import com.dowglasmaia.financecontroller.transaction.BudgetImpactPort.Impact;
import java.math.BigDecimal;
import java.time.YearMonth;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;

/** Regras do alerta ao salvar despesa (RN-BUD-06/07), com a configuração de metas em memória. */
class BudgetImpactServiceTests {
    private static final YearMonth JULY = YearMonth.of(2026, 7);
    private static final YearMonth AUGUST = YearMonth.of(2026, 8);

    private final UUID ownerId = UUID.randomUUID();
    private final UUID food = UUID.randomUUID();
    private final Config config = new Config();
    private final BudgetImpactService impact = new BudgetImpactService(config);

    private void configure(String monthly, MonthOverride julyOverride, Map<UUID, BigDecimal> categoryLimits) {
        config.monthly = monthly == null ? null : new BigDecimal(monthly);
        config.julyOverride = julyOverride;
        config.categoryLimits = categoryLimits;
    }

    private Impact impactOf(UUID category, String catBefore, String catAfter, String monthBefore, String monthAfter) {
        return new Impact(ownerId, Currency.BRL, JULY, category, "Cat", new BigDecimal(catBefore),
            new BigDecimal(catAfter), new BigDecimal(monthBefore), new BigDecimal(monthAfter));
    }

    // --- alerta (RN-BUD-06/07) ---

    @Test
    void alertTransitionsFollowTheOrigin() {
        configure("1000", null, Map.of());

        // global, categoria sem limite
        assertThat(impact.evaluate(impactOf(food, "0", "0", "700", "850"))).get()
            .isEqualTo(new BudgetAlert(BudgetAlert.Scope.GLOBAL, BudgetAlert.Status.WARNING, null, null));
        assertThat(impact.evaluate(impactOf(food, "0", "0", "850", "1100"))).get()
            .extracting(BudgetAlert::status).isEqualTo(BudgetAlert.Status.EXCEEDED);
        assertThat(impact.evaluate(impactOf(food, "0", "0", "700", "1100"))).get()
            .extracting(BudgetAlert::status).isEqualTo(BudgetAlert.Status.EXCEEDED);
        // já estourada: nunca repete
        assertThat(impact.evaluate(impactOf(food, "0", "0", "1100", "1300"))).isEmpty();
        // continua em WARNING: não repete
        assertThat(impact.evaluate(impactOf(food, "0", "0", "850", "900"))).isEmpty();
        // continua OK
        assertThat(impact.evaluate(impactOf(food, "0", "0", "100", "300"))).isEmpty();
        // melhora (edição para menos): nunca alerta
        assertThat(impact.evaluate(impactOf(food, "0", "0", "1100", "500"))).isEmpty();
    }

    @Test
    void categoryHasPriorityOverGlobalAndOnlyOneAlertIsReturned() {
        configure("1000", null, Map.of(food, new BigDecimal("200")));

        // ambos pioram: vence a categoria
        var both = impact.evaluate(impactOf(food, "100", "170", "700", "850")).orElseThrow();
        assertThat(both.scope()).isEqualTo(BudgetAlert.Scope.CATEGORY);
        assertThat(both.categoryId()).isEqualTo(food);
        assertThat(both.categoryName()).isEqualTo("Cat");

        // só o global piora
        var onlyGlobal = impact.evaluate(impactOf(food, "10", "20", "700", "850")).orElseThrow();
        assertThat(onlyGlobal.scope()).isEqualTo(BudgetAlert.Scope.GLOBAL);

        // a categoria já estava estourada (não repete) mas o global piora: alerta global
        var globalAfterCategory = impact.evaluate(impactOf(food, "250", "300", "700", "850")).orElseThrow();
        assertThat(globalAfterCategory.scope()).isEqualTo(BudgetAlert.Scope.GLOBAL);
    }

    @Test
    void noLimitMeansNoAlertAndDisabledMonthSilencesTheGlobalOne() {
        assertThat(impact.evaluate(impactOf(food, "0", "999", "0", "9999"))).isEmpty();

        configure("1000", MonthOverride.off(), Map.of());
        assertThat(impact.evaluate(impactOf(food, "0", "0", "700", "1500"))).isEmpty();
        // outro mês continua com meta
        var august = new Impact(ownerId, Currency.BRL, AUGUST, food, "Cat", BigDecimal.ZERO, BigDecimal.ZERO,
            new BigDecimal("700"), new BigDecimal("850"));
        assertThat(impact.evaluate(august)).isPresent();
    }

    @Test
    void thresholdIsExactlyEightyPercentAndAHundredPercent() {
        configure("1000", null, Map.of());

        assertThat(impact.evaluate(impactOf(food, "0", "0", "0", "799.99"))).isEmpty();
        assertThat(impact.evaluate(impactOf(food, "0", "0", "0", "800.00"))).get()
            .extracting(BudgetAlert::status).isEqualTo(BudgetAlert.Status.WARNING);
        assertThat(impact.evaluate(impactOf(food, "0", "0", "0", "999.99"))).get()
            .extracting(BudgetAlert::status).isEqualTo(BudgetAlert.Status.WARNING);
        assertThat(impact.evaluate(impactOf(food, "0", "0", "0", "1000.00"))).get()
            .extracting(BudgetAlert::status).isEqualTo(BudgetAlert.Status.EXCEEDED);
    }

    private static final class Config implements BudgetRepository {
        BigDecimal monthly;
        MonthOverride julyOverride;
        Map<UUID, BigDecimal> categoryLimits = new HashMap<>();

        @Override
        public BudgetConfig load(UUID ownerId, Currency currency, YearMonth month) {
            return new BudgetConfig(currency, monthly, JULY.equals(month) ? julyOverride : null, categoryLimits);
        }

        @Override
        public void save(UUID ownerId, Currency currency, YearMonth month, BigDecimal monthlyLimit, MonthOverride override,
                         Map<UUID, BigDecimal> limits) {
            throw new UnsupportedOperationException();
        }
    }
}
