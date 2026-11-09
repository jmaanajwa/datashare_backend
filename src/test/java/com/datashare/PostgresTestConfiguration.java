package com.datashare;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.testcontainers.containers.PostgreSQLContainer;

/** PostgreSQL reel, ephemere et isole de la base de developpement. */
@TestConfiguration(proxyBeanMethods = false)
public class PostgresTestConfiguration {
    @Bean
    @ServiceConnection
    PostgreSQLContainer<?> postgres() {
        return new PostgreSQLContainer<>("postgres:17")
                .withDatabaseName("datashare_test")
                .withUsername("datashare_test")
                .withPassword("datashare_test");
    }
}
