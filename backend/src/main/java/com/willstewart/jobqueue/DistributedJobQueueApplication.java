package com.willstewart.jobqueue;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class DistributedJobQueueApplication {

    public static void main(String[] args) {
        SpringApplication.run(DistributedJobQueueApplication.class, args);
    }
}