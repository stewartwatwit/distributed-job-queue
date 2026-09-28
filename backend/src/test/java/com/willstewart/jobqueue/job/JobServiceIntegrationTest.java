package com.willstewart.jobqueue.job;

import org.junit.jupiter.api.Test;

import com.willstewart.jobqueue.job.JobStatus.Status;
import com.willstewart.jobqueue.job.JobType.Type;

import jakarta.transaction.Transactional;
import jakarta.persistence.EntityManager;

import com.willstewart.jobqueue.dto.JobRequest;

import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

@SpringBootTest
@Testcontainers
@Transactional
@ActiveProfiles("test")
public class JobServiceIntegrationTest {

    private static final Type TEST_TYPE = Type.EMAIL;
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
    private EntityManager entityManager;

    @Autowired
    private JobService jobService;

    @Autowired
    private JobRepository jobRepository;

    private JobRequest createTestRequest() {
        JobRequest request = new JobRequest();
        request.setType(TEST_TYPE);
        request.setPayload(TEST_PAYLOAD);
        request.setPriority(TEST_PRIORITY);
        return request;
    }

    @Test 
    void shouldSubmitJobAsPending() {
        JobRequest request = createTestRequest();

        Job submittedJob = jobService.submitJob(request);

        Job retrievedJob = jobRepository.findById(submittedJob.getId()).orElse(null);

        assertEquals(submittedJob.getId(), retrievedJob.getId());

        assertEquals(Status.PENDING, submittedJob.getStatus());

        assertEquals(request.getType(), submittedJob.getType());
        assertEquals(request.getPayload(), submittedJob.getPayload());
        assertEquals(request.getPriority(), submittedJob.getPriority());
    }

    @Test
    void shouldGetJobById() {
        JobRequest request = createTestRequest();
        Job submittedJob = jobService.submitJob(request);

        Job retrievedJob = jobService.getJob(submittedJob.getId());

        assertNotNull(retrievedJob);
        assertEquals(submittedJob.getId(), retrievedJob.getId());
        assertEquals(submittedJob.getType(), retrievedJob.getType());
        assertEquals(submittedJob.getPayload(), retrievedJob.getPayload());
        assertEquals(submittedJob.getPriority(), retrievedJob.getPriority());
    }

    @Test 
    void shouldThrowExceptionWhenJobDoesNotExist() {
        UUID testId = UUID.randomUUID();

        assertThrows(RuntimeException.class, () -> {
            jobService.getJob(testId);
        });
    }

    @Test 
    void shouldCompleteJob() {
        JobRequest request = createTestRequest();
        Job submittedJob = jobService.submitJob(request);

        submittedJob.setStatus(Status.RUNNING);

        jobRepository.saveAndFlush(submittedJob);
        entityManager.clear();

        jobService.completeJob(submittedJob.getId());

        Job completedJob = jobService.getJob(submittedJob.getId());
        assertEquals(Status.COMPLETED, completedJob.getStatus());
        assertNotNull(completedJob.getEndedAt());
    }

    @Test 
    void shouldNotCompleteNonRunningJob() {
        JobRequest request = createTestRequest();
        Job submittedJob = jobService.submitJob(request);

        submittedJob.setStatus(Status.PENDING);

        jobRepository.saveAndFlush(submittedJob);
        entityManager.clear();

        assertThrows(RuntimeException.class, () -> {
            jobService.completeJob(submittedJob.getId());
        });
    }

    @Test
    void shouldSetFailedJobToPendingWithRemainingAttempts() {
        JobRequest request = createTestRequest();
        Job submittedJob = jobService.submitJob(request);

        submittedJob.setStatus(Status.RUNNING);

        jobRepository.saveAndFlush(submittedJob);
        entityManager.clear();

        jobService.failJob(submittedJob.getId());

        Job failedJob = jobService.getJob(submittedJob.getId());
        assertEquals(Status.PENDING, failedJob.getStatus());
    }

    @Test
    void shouldMarkJobFailedWhenMaxAttemptsReached() {
        JobRequest request = createTestRequest();
        Job submittedJob = jobService.submitJob(request);

        submittedJob.setStatus(Status.RUNNING);

        submittedJob.setAttemptCount(5);

        jobRepository.saveAndFlush(submittedJob);
        entityManager.clear();

        jobService.failJob(submittedJob.getId());

        Job failedJob = jobService.getJob(submittedJob.getId());
        assertEquals(Status.FAILED, failedJob.getStatus());
        assertNotNull(failedJob.getEndedAt());
    }

    @Test 
    void shouldNotFailNonRunningJob() {
        JobRequest request = createTestRequest();
        Job submittedJob = jobService.submitJob(request);

        submittedJob.setStatus(Status.PENDING);

        jobRepository.saveAndFlush(submittedJob);
        entityManager.clear();

        assertThrows(RuntimeException.class, () -> {
            jobService.failJob(submittedJob.getId());
        });
    }
}
