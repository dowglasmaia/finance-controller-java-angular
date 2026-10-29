package com.dowglasmaia.financecontroller.category.application.listener;

import com.dowglasmaia.financecontroller.category.application.usecase.SeedDefaultCategoriesUseCase;
import com.dowglasmaia.financecontroller.identity.UserRegistered;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * Reage ao cadastro de forma síncrona, na mesma transação: se a semente falhar, o cadastro também é desfeito.
 * Evento em vez de chamada direta para que {@code identity} não dependa de {@code category}.
 */
@Component
class SeedCategoriesOnUserRegistered {

    private final SeedDefaultCategoriesUseCase seed;

    SeedCategoriesOnUserRegistered(SeedDefaultCategoriesUseCase seed) {
        this.seed = seed;
    }

    @EventListener
    void on(UserRegistered event) {
        seed.execute(event.userId());
    }
}
