package com.willstewart.jobqueue.job;

import com.willstewart.jobqueue.dto.JobRequest;
import com.willstewart.jobqueue.job.JobStatus.Status;
import com.willstewart.jobqueue.job.JobType.Type;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class JobTest {
    private static final Type TEST_TYPE = Type.EMAIL;
    private static final Status TEST_STATUS = Status.RUNNING;
    private static final String TEST_PAYLOAD = "{\"test\": \"value\"}";
    private static final int TEST_PRIORITY = 1;

    private JobRequest createTestRequest() {
        JobRequest request = new JobRequest();
        request.setType(TEST_TYPE);
        request.setPayload(TEST_PAYLOAD);
        request.setPriority(TEST_PRIORITY);
        return request;
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
}
