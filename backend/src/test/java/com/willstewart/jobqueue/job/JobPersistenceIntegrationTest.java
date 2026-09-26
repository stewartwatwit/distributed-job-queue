package com.willstewart.jobqueue.job;

import org.junit.jupiter.api.Test;
import org.testcontainers.junit.jupiter.Testcontainers;

import com.willstewart.jobqueue.dto.JobRequest;
import com.willstewart.jobqueue.job.JobStatus.Status;
import com.willstewart.jobqueue.job.JobType.Type;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import jakarta.persistence.EntityManager;
import jakarta.transaction.Transactional;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@Testcontainers
@SpringBootTest
@Transactional
@ActiveProfiles("test")
public class JobPersistenceIntegrationTest {

    private static final Type TEST_TYPE = Type.EMAIL;

    private static final Status TEST_STATUS = Status.RUNNING;

    private static final String TEST_PAYLOAD = "{\"test\": \"value\"}";

    private static final int TEST_PRIORITY = 1;

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

}
