package com.willstewart.jobqueue;

import org.junit.jupiter.api.Test;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.junit.jupiter.api.Assertions.assertTrue;

@Testcontainers
public class TestcontainersTest {
    
    @Container
    static PostgreSQLContainer<?> postgres =
        new PostgreSQLContainer<>("postgres:18")
            .withDatabaseName("testdb")
            .withUsername("testuser")
            .withPassword("M9q6l65b!");

    @Test
    void postgresContainerStarts() {
        assertTrue(postgres.isRunning());
    }

}
