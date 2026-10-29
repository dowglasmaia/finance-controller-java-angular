package com.dowglasmaia.financecontroller.support;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.testcontainers.postgresql.PostgreSQLContainer;

/** Real PostgreSQL shared by every Spring context in the test run; wired as the datasource via @ServiceConnection. */
@TestConfiguration(proxyBeanMethods = false)
public class PostgresContainerConfiguration {
    public static final String IMAGE = "postgres:18.1";

    // Cada contexto Spring em cache mantém o seu próprio pool: com muitas configurações distintas o padrão (100 conexões) esgota.
    private static final PostgreSQLContainer POSTGRES = new PostgreSQLContainer(IMAGE)
        .withCommand("postgres", "-c", "max_connections=400");

    static {
        POSTGRES.start();
    }

    /** O contentor compartilhado, para testes que precisam de outro banco ou de outros utilizadores nele. */
    public static PostgreSQLContainer container() {
        return POSTGRES;
    }

    @Bean
    @ServiceConnection
    PostgreSQLContainer postgresContainer() {
        return POSTGRES;
    }
}
