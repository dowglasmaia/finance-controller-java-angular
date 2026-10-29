package com.dowglasmaia.financecontroller.identity.application.port.out;

import java.time.Duration;

/** Configuração da recuperação de senha; a base do link vem sempre da configuração, nunca do pedido. */
public interface PasswordResetSettings {

    Duration tokenTtl();

    String appBaseUrl();
}
