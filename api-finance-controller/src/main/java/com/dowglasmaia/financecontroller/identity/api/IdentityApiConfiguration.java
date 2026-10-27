package com.dowglasmaia.financecontroller.identity.api;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(AuthCookieProperties.class)
class IdentityApiConfiguration {
}
