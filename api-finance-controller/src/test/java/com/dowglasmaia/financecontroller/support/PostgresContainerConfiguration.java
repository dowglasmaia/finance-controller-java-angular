package com.dowglasmaia.financecontroller.support;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.testcontainers.postgresql.PostgreSQLContainer;

/** Real PostgreSQL shared by every Spring context in the test run; wired as the datasource via @ServiceConnection. */
@TestConfiguration(proxyBeanMethods = false)
public class PostgresContainerConfiguration {
    public static final String IMAGE = "postgres:18.1";

    private static final PostgreSQLContainer POSTGRES = new PostgreSQLContainer(IMAGE);

    static {
        POSTGRES.start();
    }

    @Bean
    @ServiceConnection
    PostgreSQLContainer postgresContainer() {
        return POSTGRES;
    }
}
