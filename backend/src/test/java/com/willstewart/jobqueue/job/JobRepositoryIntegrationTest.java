package com.willstewart.jobqueue.job;

import com.willstewart.jobqueue.dto.JobRequest;
import com.willstewart.jobqueue.job.JobStatus.Status;
import com.willstewart.jobqueue.job.JobType.Type;

import jakarta.persistence.EntityManager;
import jakarta.transaction.Transactional;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.Instant;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Testcontainers
@SpringBootTest
@Transactional
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

    private JobRequest createTestRequest() {
        JobRequest request = new JobRequest();

        request.setType(TEST_TYPE);
        request.setPayload(TEST_PAYLOAD);
        request.setPriority(TEST_PRIORITY);

        return request;
    }

    @Test
    void shouldSaveAndRetrieveJob() {
        Job job = new Job(createTestRequest());

        Job savedJob = jobRepository.saveAndFlush(job);

        entityManager.clear();

        assertNotNull(savedJob.getId());

        Job retrievedJob = jobRepository.findById(job.getId()).orElseThrow();

        assertEquals(savedJob.getId(), retrievedJob.getId());
        assertEquals(savedJob.getType(), retrievedJob.getType());
        assertEquals(savedJob.getStatus(), retrievedJob.getStatus());
        assertEquals(savedJob.getPayload(), retrievedJob.getPayload());
        assertEquals(savedJob.getCreatedAt(), retrievedJob.getCreatedAt());
        assertEquals(savedJob.getUpdatedAt(), retrievedJob.getUpdatedAt());
        assertEquals(savedJob.getPriority(), retrievedJob.getPriority());
        assertEquals(savedJob.getAttemptCount(), retrievedJob.getAttemptCount());
        assertEquals(savedJob.getStartedAt(), retrievedJob.getStartedAt());
        assertEquals(savedJob.getEndedAt(), retrievedJob.getEndedAt());
    }

    @Test
    void generatedUUIDsShouldBeUnique() {
        Job job1 = new Job(createTestRequest());
        Job job2 = new Job(createTestRequest());

        jobRepository.saveAndFlush(job1);
        jobRepository.saveAndFlush(job2);

        assertNotEquals(job1.getId(), job2.getId());
    }

    @Test
    void shouldInitializeNewJobWithExpectedDefaults() {
        Job job = new Job(createTestRequest());

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
        Job job = new Job(createTestRequest());

        assertEquals(Status.PENDING, job.getStatus());

        job.setStatus(TEST_STATUS);

        assertEquals(TEST_STATUS, job.getStatus());
    }

    @Test
    void shouldTimestampWhenJobIsPersisted() {
        Job job = new Job(createTestRequest());

        Job savedJob = jobRepository.saveAndFlush(job);

        entityManager.clear();

        assertNotNull(savedJob.getCreatedAt());
        assertNotNull(savedJob.getUpdatedAt());
        assertEquals(savedJob.getCreatedAt(), savedJob.getUpdatedAt());
    }

    @Test
    void shouldUpdateTimestampWhenJobIsModified() {
        Job job = new Job(createTestRequest());

        Job savedJob = jobRepository.saveAndFlush(job);

        Instant firstTime = savedJob.getUpdatedAt();

        savedJob.setStatus(TEST_STATUS);

        Job updatedJob = jobRepository.saveAndFlush(savedJob);

        Instant secondTime = updatedJob.getUpdatedAt();

        assertNotNull(secondTime);
        assertTrue(
            firstTime.isBefore(secondTime) ||
            firstTime.equals(secondTime)
        );
    }

    /*
     * Tests for the findNextPendingJob method in the JobRepository.
     */

    @Test
    void shouldFindNextPendingJob() {
        Job jobA = new Job(createTestRequest());

        JobRequest highPriorityRequest = createTestRequest();
        highPriorityRequest.setPriority(10);
        Job jobB = new Job(highPriorityRequest);

        JobRequest mediumPriorityRequest = createTestRequest();
        mediumPriorityRequest.setPriority(5);
        Job jobC = new Job(mediumPriorityRequest);

        jobRepository.saveAndFlush(jobA);
        jobRepository.saveAndFlush(jobB);
        jobRepository.saveAndFlush(jobC);

        entityManager.clear();

        Optional<Job> nextPendingJob = jobRepository.findNextPendingJob();

        assertTrue(nextPendingJob.isPresent());
        assertEquals(jobB.getId(), nextPendingJob.get().getId());
    }

    @Test
    void ifEqualPriorityJobsShouldReturnOldest() {
        Job jobA = new Job(createTestRequest());
        Job jobB = new Job(createTestRequest());

        jobRepository.saveAndFlush(jobA);
        jobRepository.saveAndFlush(jobB);

        entityManager.createNativeQuery(
            "UPDATE jobs SET created_at = :createdAt WHERE id = :id"
        )
        .setParameter(
            "createdAt",
            Instant.parse("2026-01-01T10:00:00Z")
        )
        .setParameter("id", jobA.getId())
        .executeUpdate();

        entityManager.createNativeQuery(
            "UPDATE jobs SET created_at = :createdAt WHERE id = :id"
        )
        .setParameter(
            "createdAt",
            Instant.parse("2026-01-01T11:00:00Z")
        )
        .setParameter("id", jobB.getId())
        .executeUpdate();

        entityManager.flush();
        entityManager.clear();

        Optional<Job> nextPendingJob = jobRepository.findNextPendingJob();

        assertTrue(nextPendingJob.isPresent());
        assertEquals(jobA.getId(), nextPendingJob.get().getId());
    }

    @Test
    void nonPendingJobsShouldNotBeReturned() {
        JobRequest highPriorityRequest = createTestRequest();
        highPriorityRequest.setPriority(10);

        Job jobA = new Job(highPriorityRequest);
        jobA.setStatus(Status.COMPLETED);

        JobRequest lowPriorityRequest = createTestRequest();
        lowPriorityRequest.setPriority(1);

        Job jobB = new Job(lowPriorityRequest);

        jobRepository.saveAndFlush(jobA);
        jobRepository.saveAndFlush(jobB);

        entityManager.clear();

        Optional<Job> nextPendingJob = jobRepository.findNextPendingJob();

        assertTrue(nextPendingJob.isPresent());
        assertEquals(jobB.getId(), nextPendingJob.get().getId());
    }

    @Test
    void shouldReturnEmptyWhenNoJobsExist() {
        entityManager.clear();

        Optional<Job> nextPendingJob = jobRepository.findNextPendingJob();

        assertTrue(nextPendingJob.isEmpty());
    }
}
