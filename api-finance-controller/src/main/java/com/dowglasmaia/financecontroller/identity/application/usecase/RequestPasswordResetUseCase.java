package com.dowglasmaia.financecontroller.identity.application.usecase;

import com.dowglasmaia.financecontroller.identity.domain.Email;
import org.springframework.stereotype.Service;

/**
 * Pedido de recuperação: só valida o formato do e-mail e entrega o trabalho ao processador assíncrono. A resposta é
 * sempre a mesma (o controller devolve 202 com mensagem neutra), exista a conta ou não.
 */
@Service
public class RequestPasswordResetUseCase {

    private final PasswordResetProcessor processor;

    RequestPasswordResetUseCase(PasswordResetProcessor processor) {
        this.processor = processor;
    }

    /** @throws com.dowglasmaia.financecontroller.shared.error.DomainException {@code VALIDATION_FAILED} se o e-mail é inválido */
    public void execute(String email, String origin) {
        processor.requestLink(Email.of(email), origin);
    }
}
