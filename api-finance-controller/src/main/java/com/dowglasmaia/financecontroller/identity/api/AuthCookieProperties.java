package com.dowglasmaia.financecontroller.identity.api;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/**
 * Atributos do cookie de renovação ({@code app.identity.cookie.*}). O nome precisa coincidir com o parâmetro
 * {@code refresh_token} do contrato; só é configurável para manter contrato e cookie alinhados.
 */
@ConfigurationProperties("app.identity.cookie")
public record AuthCookieProperties(
        @DefaultValue("refresh_token") String name,
        @DefaultValue("true") boolean secure,
        @DefaultValue("Lax") String sameSite,
        @DefaultValue("/api/v1/auth") String path) {
}
