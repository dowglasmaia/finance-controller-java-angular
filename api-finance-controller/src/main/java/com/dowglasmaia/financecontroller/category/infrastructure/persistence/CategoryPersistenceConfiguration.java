package com.dowglasmaia.financecontroller.category.infrastructure.persistence;

import com.dowglasmaia.financecontroller.shared.error.ConstraintErrorMapping;
import com.dowglasmaia.financecontroller.shared.error.ErrorCode;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
class CategoryPersistenceConfiguration {

    @Bean
    ConstraintErrorMapping categoryNameDuplicatedMapping() {
        return new ConstraintErrorMapping("uk_category_owner_name", ErrorCode.CATEGORY_NAME_DUPLICATED);
    }

    @Bean
    ConstraintErrorMapping subcategoryNameDuplicatedMapping() {
        return new ConstraintErrorMapping("uk_subcategory_category_name", ErrorCode.SUBCATEGORY_NAME_DUPLICATED);
    }
}
