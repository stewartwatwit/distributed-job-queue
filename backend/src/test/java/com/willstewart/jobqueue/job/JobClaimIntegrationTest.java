package com.willstewart.jobqueue.job;

import com.willstewart.jobqueue.job.JobType.Type;
import com.willstewart.jobqueue.dto.JobRequest;
import com.willstewart.jobqueue.job.JobStatus.Status;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;


import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.junit.jupiter.Container;

import java.util.Optional;

import jakarta.persistence.EntityManager;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Testcontainers 
@SpringBootTest
@ActiveProfiles("test")
public class JobClaimIntegrationTest {

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
    private JobService jobService;

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

    @BeforeEach
    void cleanDatabase() {
        jobRepository.deleteAll();
        jobRepository.flush();
        entityManager.clear();
    }

    @Test
    void shouldClaimPendingJob() {
        JobRequest request = createTestRequest();
        Job job = new Job(request);

        jobRepository.saveAndFlush(job);
        entityManager.clear();

        Job claimedJob = jobService.claimNextJob().orElseThrow(() -> 
            new RuntimeException("No job claimed")
        );

        entityManager.clear();

        Job persistedJob = jobRepository.findById(claimedJob.getId()).orElseThrow(() -> 
            new RuntimeException("Claimed job not found")
        );

        assertNotNull(persistedJob);
        assertEquals(job.getId(), persistedJob.getId());
    }

    @Test 
    void shouldSetClaimedJobToRunning() {
        JobRequest request = createTestRequest();
        Job job = new Job(request);

        jobRepository.saveAndFlush(job);
        entityManager.clear();

        Job claimedJob = jobService.claimNextJob().orElseThrow(() -> 
            new RuntimeException("No job claimed")
        );

        entityManager.clear();

        Job persistedJob = jobRepository.findById(claimedJob.getId()).orElseThrow(() -> 
            new RuntimeException("Claimed job not found")
        );

        assertNotNull(persistedJob);
        assertEquals(Status.RUNNING, persistedJob.getStatus());
    }

    @Test 
    void shouldSetStartedAtWhenClaimed() {
        JobRequest request = createTestRequest();
        Job job = new Job(request);

        jobRepository.saveAndFlush(job);
        entityManager.clear();

        Job claimedJob = jobService.claimNextJob().orElseThrow(() -> 
            new RuntimeException("No job claimed")
        );

        entityManager.clear();

        Job persistedJob = jobRepository.findById(claimedJob.getId()).orElseThrow(() -> 
            new RuntimeException("Claimed job not found")
        );

        assertNotNull(persistedJob);
        assertNotNull(persistedJob.getStartedAt());
    }

    @Test 
    void shouldReturnEmptyWhenNoPendingJobs() {
        Optional<Job> claimedJob = jobService.claimNextJob();
        assertTrue(claimedJob.isEmpty());
    }

    @Test 
    void shouldClaimHighestPriorityJob() {
        JobRequest lowPriorityRequest = createTestRequest();
        lowPriorityRequest.setPriority(1);
        Job lowPriorityJob = new Job(lowPriorityRequest);

        JobRequest highPriorityRequest = createTestRequest();
        highPriorityRequest.setPriority(10);
        Job highPriorityJob = new Job(highPriorityRequest);

        jobRepository.saveAndFlush(lowPriorityJob);
        jobRepository.saveAndFlush(highPriorityJob);
        entityManager.clear();

        Job claimedJob = jobService.claimNextJob().orElseThrow(() -> 
            new RuntimeException("No job claimed")
        );

        entityManager.clear();

        Job persistedJob = jobRepository.findById(claimedJob.getId()).orElseThrow(() -> 
            new RuntimeException("Claimed job not found")
        );

        assertNotNull(persistedJob);
        assertEquals(highPriorityJob.getId(), persistedJob.getId());
    }

    @Test 
    void shouldNotClaimCompletedJob() {
        JobRequest runningRequest = createTestRequest();
        Job runningJob = new Job(runningRequest);

        JobRequest completedRequest = createTestRequest();
        Job completedJob = new Job(completedRequest);
        completedJob.setStatus(Status.COMPLETED);

        jobRepository.saveAndFlush(runningJob);
        jobRepository.saveAndFlush(completedJob);
        entityManager.clear();

        Job claimedJob = jobService.claimNextJob().orElseThrow(() -> 
            new RuntimeException("No job claimed")
        );

        entityManager.clear();

        Job persistedJob = jobRepository.findById(claimedJob.getId()).orElseThrow(() -> 
            new RuntimeException("Claimed job not found")
        );

        assertNotNull(persistedJob);
        assertEquals(runningJob.getId(), persistedJob.getId());
    }

    @Test 
    void shouldNotClaimFailedJob() {
        JobRequest runningRequest = createTestRequest();
        Job runningJob = new Job(runningRequest);

        JobRequest failedRequest = createTestRequest();
        Job failedJob = new Job(failedRequest);
        failedJob.setStatus(Status.FAILED);

        jobRepository.saveAndFlush(runningJob);
        jobRepository.saveAndFlush(failedJob);
        entityManager.clear();

        Job claimedJob = jobService.claimNextJob().orElseThrow(() -> 
            new RuntimeException("No job claimed")
        );

        entityManager.clear();

        Job persistedJob = jobRepository.findById(claimedJob.getId()).orElseThrow(() -> 
            new RuntimeException("Claimed job not found")
        );

        assertNotNull(persistedJob);
        assertEquals(runningJob.getId(), persistedJob.getId());
    }
}
