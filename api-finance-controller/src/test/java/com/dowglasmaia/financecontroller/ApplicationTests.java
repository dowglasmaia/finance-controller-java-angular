package com.dowglasmaia.financecontroller;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import com.dowglasmaia.financecontroller.support.PostgresContainerConfiguration;

@SpringBootTest
@Import(PostgresContainerConfiguration.class)
class ApplicationTests {
    @Test
    void contextLoads() {
    }
}
