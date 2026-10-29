package com.dowglasmaia.financecontroller.transaction.infrastructure.persistence;

import com.dowglasmaia.financecontroller.shared.error.ConstraintErrorMapping;
import com.dowglasmaia.financecontroller.shared.error.ErrorCode;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
class TransactionPersistenceConfiguration {

    /**
     * Corrida entre excluir uma categoria e lançar nela: o banco recusa a exclusão (ou o lançamento) e o cliente vê
     * "categoria em uso", nunca um erro interno.
     */
    @Bean
    ConstraintErrorMapping transactionCategoryInUseMapping() {
        return new ConstraintErrorMapping("fk_transaction_entry_category", ErrorCode.CATEGORY_IN_USE);
    }
}
