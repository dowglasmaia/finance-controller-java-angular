package com.dowglasmaia.financecontroller.budget.infrastructure.persistence;

import com.dowglasmaia.financecontroller.shared.error.ConstraintErrorMapping;
import com.dowglasmaia.financecontroller.shared.error.ErrorCode;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
class BudgetPersistenceConfiguration {

    /** A categoria foi excluída entre a validação e a gravação: o cliente vê "não encontrado", não um erro interno. */
    @Bean
    ConstraintErrorMapping budgetCategoryRemovedMapping() {
        return new ConstraintErrorMapping("fk_budget_category_limit_category", ErrorCode.RESOURCE_NOT_FOUND);
    }
}
