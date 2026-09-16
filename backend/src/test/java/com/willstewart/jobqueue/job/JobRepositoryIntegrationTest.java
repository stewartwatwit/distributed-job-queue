package com.willstewart.jobqueue.job;

import org.junit.jupiter.api.Test;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.junit.jupiter.Container;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertEquals;

@Testcontainers
@SpringBootTest 
@ActiveProfiles("test")
public class JobRepositoryIntegrationTest {
    
    @Container
    static PostgreSQLContainer<?> postgresContainer = 
        new PostgreSQLContainer<>("postgres:18")
        .withDatabaseName("testdb")
        .withUsername("testuser")
        .withPassword("testpassword");

    @DynamicPropertySource
    static void registerPgProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgresContainer::getJdbcUrl);
        registry.add("spring.datasource.username", postgresContainer::getUsername);
        registry.add("spring.datasource.password", postgresContainer::getPassword);
    }

    @Autowired
    private JobRepository jobRepository;

    @Test
    void shouldSaveAndRetrieveJob() {
        Job job = new Job("QUEUED");

        Job savedJob = jobRepository.save(job);

        assertNotNull(savedJob.getId());

        Job retrievedJob = jobRepository.findById(job.getId()).orElseThrow();

        assertEquals(savedJob.getId(), retrievedJob.getId());

        assertEquals("QUEUED", retrievedJob.getStatus());
    }

}
