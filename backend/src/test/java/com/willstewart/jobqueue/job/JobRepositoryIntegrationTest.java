package com.willstewart.jobqueue.job;

import org.junit.jupiter.api.Test;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.junit.jupiter.Container;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import jakarta.persistence.EntityManager;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;

import java.time.Instant;

import com.willstewart.jobqueue.job.JobStatus.Status;
import com.willstewart.jobqueue.job.JobType.Type;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Testcontainers
@SpringBootTest 
@ActiveProfiles("test")
public class JobRepositoryIntegrationTest {

    private static final Type TEST_TYPE = Type.EMAIL;

    private static final Status TEST_STATUS = Status.RUNNING;

    private static final String TEST_PAYLOAD = "{\"test\": \"value\"}";

    private static final int TEST_PRIORITY = 1;

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

    @Autowired
    private EntityManager entityManager;

    @Test
    void shouldSaveAndRetrieveJob() {
        Job job = new Job(TEST_TYPE, TEST_PAYLOAD, TEST_PRIORITY);

        Job savedJob = jobRepository.saveAndFlush(job);

        entityManager.clear();

        assertNotNull(savedJob.getId());

        Job retrievedJob = jobRepository.findById(job.getId()).orElseThrow();

        assertEquals(savedJob.getId(), retrievedJob.getId());
        assertEquals(savedJob.getType(), retrievedJob.getType());
        assertEquals(savedJob.getStatus(),retrievedJob.getStatus());
        assertEquals(savedJob.getPayload(),retrievedJob.getPayload());
        assertEquals(savedJob.getCreatedAt(),retrievedJob.getCreatedAt());
        assertEquals(savedJob.getUpdatedAt(),retrievedJob.getUpdatedAt());
        assertEquals(savedJob.getPriority(),retrievedJob.getPriority());
        assertEquals(savedJob.getAttemptCount(),retrievedJob.getAttemptCount());
        assertEquals(savedJob.getStartedAt(),retrievedJob.getStartedAt());
        assertEquals(savedJob.getEndedAt(),retrievedJob.getEndedAt());
    }

    @Test 
    void generatedUUIDsShouldBeUnique() {
        Job job1 = new Job(TEST_TYPE, TEST_PAYLOAD, TEST_PRIORITY);
        Job job2 = new Job(TEST_TYPE, TEST_PAYLOAD, TEST_PRIORITY);

        jobRepository.saveAndFlush(job1);
        jobRepository.saveAndFlush(job2);

        assertNotEquals(job1.getId(), job2.getId());
    }
    
    @Test 
    void shouldInitializeNewJobWithExpectedDefaults() {
        Job job = new Job(TEST_TYPE, TEST_PAYLOAD, TEST_PRIORITY);

        assertEquals(TEST_TYPE, job.getType());
        assertEquals(TEST_PAYLOAD, job.getPayload());
        assertEquals(TEST_PRIORITY, job.getPriority());

        assertEquals(Status.PENDING, job.getStatus());
        assertEquals(0, job.getAttemptCount());
        assertNull(job.getStartedAt());
        assertNull(job.getEndedAt());
    }

    @Test
    void shouldSetAndUpdateJobStatus() {
        Job job = new Job(TEST_TYPE, TEST_PAYLOAD, TEST_PRIORITY);

        assertEquals(Status.PENDING, job.getStatus());

        job.setStatus(TEST_STATUS);

        assertEquals(TEST_STATUS, job.getStatus());
    }

    @Test 
    void shouldTimestampWhenJobIsPersisted() {
        Job job = new Job(TEST_TYPE, TEST_PAYLOAD, TEST_PRIORITY);

        Job check = jobRepository.saveAndFlush(job);

        entityManager.clear();

        assertNotNull(check.getCreatedAt());
        assertNotNull(check.getUpdatedAt());
        assertEquals(check.getCreatedAt(), check.getUpdatedAt());
    }

    @Test 
    void shouldUpdateTimestampWhenJobIsModified() {
        Job job = new Job(TEST_TYPE, TEST_PAYLOAD, TEST_PRIORITY);

        jobRepository.saveAndFlush(job);

        entityManager.clear();

        Instant firstTime = job.getUpdatedAt();

        job.setStatus(TEST_STATUS);

        jobRepository.saveAndFlush(job);

        Instant secondTime = job.getUpdatedAt();
        
        assertNotNull(secondTime);
        assertTrue(firstTime.isBefore(secondTime) || firstTime.equals(secondTime));
    }

}
