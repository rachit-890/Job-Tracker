package com.rachit.jobtrackr;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.retry.annotation.EnableRetry;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableRetry       // activates @Retryable on Gemini service methods
@EnableScheduling  // needed for Phase 5 scheduled jobs — wiring it now
public class JobtrackrApplication {
    public static void main(String[] args) {
        SpringApplication.run(JobtrackrApplication.class, args);
    }
}