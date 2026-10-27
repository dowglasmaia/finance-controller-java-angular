package com.dowglasmaia.financecontroller.support;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

/** Base for integration tests: full context, Flyway applied to a throwaway PostgreSQL (never H2). */
@SpringBootTest
@Import(PostgresContainerConfiguration.class)
public abstract class AbstractPostgresIntegrationTest {
}
