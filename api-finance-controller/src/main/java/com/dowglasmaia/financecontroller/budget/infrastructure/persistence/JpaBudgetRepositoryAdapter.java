package com.dowglasmaia.financecontroller.budget.infrastructure.persistence;

import com.dowglasmaia.financecontroller.budget.application.port.out.BudgetRepository;
import com.dowglasmaia.financecontroller.budget.domain.BudgetConfig;
import com.dowglasmaia.financecontroller.budget.domain.MonthOverride;
import com.dowglasmaia.financecontroller.shared.money.Currency;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.YearMonth;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Repository
class JpaBudgetRepositoryAdapter implements BudgetRepository {

    private final SpringDataBudgetSettingRepository settings;
    private final SpringDataBudgetOverrideRepository overrides;
    private final SpringDataBudgetCategoryLimitRepository categoryLimits;

    @PersistenceContext
    private EntityManager entityManager;

    JpaBudgetRepositoryAdapter(SpringDataBudgetSettingRepository settings,
                               SpringDataBudgetOverrideRepository overrides,
                               SpringDataBudgetCategoryLimitRepository categoryLimits) {
        this.settings = settings;
        this.overrides = overrides;
        this.categoryLimits = categoryLimits;
    }

    @Override
    @Transactional(readOnly = true)
    public BudgetConfig load(UUID ownerId, Currency currency, YearMonth month) {
        BigDecimal monthly = settings.find(ownerId, currency.name()).map(BudgetEntities.Setting::getMonthlyLimit).orElse(null);
        MonthOverride override = overrides.find(ownerId, currency.name(), month.toString())
            .map(o -> o.isDisabled() ? MonthOverride.off() : MonthOverride.ofLimit(o.getLimitAmount()))
            .orElse(null);
        Map<UUID, BigDecimal> limits = new HashMap<>();
        categoryLimits.findAll(ownerId, currency.name())
            .forEach(c -> limits.put(c.getCategoryId(), c.getLimitAmount()));
        return new BudgetConfig(currency, monthly, override, limits);
    }

    @Override
    @Transactional
    public void save(UUID ownerId, Currency currency, YearMonth month, BigDecimal monthlyLimit, MonthOverride override,
                     Map<UUID, BigDecimal> limits) {
        lock(ownerId, currency);
        String code = currency.name();

        var setting = settings.find(ownerId, code);
        if (monthlyLimit == null) {
            setting.ifPresent(settings::delete);
        } else if (setting.isPresent()) {
            setting.get().setMonthlyLimit(monthlyLimit);
        } else {
            settings.save(new BudgetEntities.Setting(ownerId, code, monthlyLimit));
        }

        var existing = overrides.find(ownerId, code, month.toString());
        if (override == null) {
            existing.ifPresent(overrides::delete);
        } else {
            var entity = existing.orElseGet(() -> new BudgetEntities.MonthOverride(ownerId, code, month.toString()));
            entity.set(override.limit(), override.disabled());
            overrides.save(entity);
        }

        // delete em massa executado na hora, depois as inserções do conjunto novo
        categoryLimits.deleteAll(ownerId, code);
        limits.forEach((categoryId, limit) -> categoryLimits.save(new BudgetEntities.CategoryLimit(ownerId, code, categoryId, limit)));
        categoryLimits.flush();
    }

    /**
     * Serializa gravações simultâneas da mesma moeda do mesmo utilizador (lock consultivo até o fim da transação), para
     * que "apagar e inserir" o conjunto de limites nunca colida com outra gravação.
     */
    private void lock(UUID ownerId, Currency currency) {
        entityManager.createNativeQuery(
                "select 1 from (select pg_advisory_xact_lock(hashtextextended(:key, 0))) as locked")
            .setParameter("key", "budget:" + ownerId + ":" + currency.name())
            .getResultList();
    }
}
