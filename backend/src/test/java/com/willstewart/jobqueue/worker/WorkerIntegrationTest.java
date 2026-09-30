package com.willstewart.jobqueue.worker;

import com.willstewart.jobqueue.job.Job;
import com.willstewart.jobqueue.dto.JobRequest;
import com.willstewart.jobqueue.job.JobStatus.Status;
import com.willstewart.jobqueue.job.JobType.Type;
import com.willstewart.jobqueue.job.JobRepository;
import com.willstewart.jobqueue.job.JobService;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.ActiveProfiles;

import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers; 

import jakarta.persistence.EntityManager;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@SpringBootTest
@Testcontainers
@ActiveProfiles("test")
public class WorkerIntegrationTest {

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
    private JobRepository jobRepository; 

    @Autowired
    private JobService jobService;

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
    void shouldProcessPendingJob() throws InterruptedException {
        Job job = jobService.submitJob(createTestRequest());

        Worker worker = new Worker(jobService);

        Thread workerThread = new Thread(worker);
        workerThread.start();
        
        Thread.sleep(500);

        workerThread.interrupt();
        workerThread.join(1000);

        entityManager.clear();

        Job processedJob = jobRepository.findById(job.getId()).orElseThrow();

        assertEquals(Status.COMPLETED, processedJob.getStatus());
        assertNotNull(processedJob.getStartedAt());
        assertNotNull(processedJob.getEndedAt());
    }

}