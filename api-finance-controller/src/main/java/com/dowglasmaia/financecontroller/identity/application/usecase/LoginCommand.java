package com.dowglasmaia.financecontroller.identity.application.usecase;

/** {@code origin} identifica o cliente (endereço remoto) para a limitação de tentativas. */
public record LoginCommand(String email, String password, String origin) {
}
