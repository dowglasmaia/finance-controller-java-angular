package com.dowglasmaia.financecontroller.identity.infrastructure.persistence;

import com.dowglasmaia.financecontroller.shared.error.ConstraintErrorMapping;
import com.dowglasmaia.financecontroller.shared.error.ErrorCode;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
class IdentityPersistenceConfiguration {

    @Bean
    ConstraintErrorMapping emailAlreadyRegisteredMapping() {
        return new ConstraintErrorMapping("uq_app_user_email", ErrorCode.EMAIL_ALREADY_REGISTERED);
    }
}
