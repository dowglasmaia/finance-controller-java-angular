package com.dowglasmaia.financecontroller.shared.error;

import java.util.Locale;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.MessageSource;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.support.ResourceBundleMessageSource;

@Configuration(proxyBeanMethods = false)
public class ErrorHandlingConfiguration {
    public static final Locale PT_BR = Locale.of("pt", "BR");

    /**
     * pt-BR is the only language, so any requested locale falls back to it. Also feeds Bean Validation's
     * {@code {key}} messages through Spring Boot's validator.
     */
    @Bean
    public MessageSource messageSource() {
        var source = new ResourceBundleMessageSource();
        source.setBasename("messages");
        source.setDefaultEncoding("UTF-8");
        source.setFallbackToSystemLocale(false);
        source.setDefaultLocale(PT_BR);
        return source;
    }

    @Bean
    public ConstraintErrorRegistry constraintErrorRegistry(ObjectProvider<ConstraintErrorMapping> mappings) {
        return new ConstraintErrorRegistry(mappings.orderedStream().toList());
    }
}
