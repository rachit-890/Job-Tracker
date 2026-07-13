package com.rachit.jobtrackr.metrics;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

/**
 * Central metrics registry for JobTrackr business metrics.
 *
 * Why a dedicated class instead of injecting MeterRegistry everywhere?
 * Keeps metric names consistent — all metric names are defined in one place.
 * Services call e.g. metricsService.recordGeminiCall() rather than building
 * timer/counter names inline, which avoids typos and naming drift.
 *
 * Metrics exposed at /actuator/prometheus for Prometheus scraping.
 */
@Component
public class MetricsService {

    // Counters
    private final Counter applicationsCreated;
    private final Counter statusChanges;
    private final Counter aiTagExtractionSuccess;
    private final Counter aiTagExtractionFailure;
    private final Counter embeddingCacheHits;
    private final Counter embeddingCacheMisses;
    private final Counter emailsIngested;
    private final Counter remindersSent;
    private final Counter remindersFailed;
    private final Counter staleApplicationsFlagged;

    // Timers
    private final Timer geminiChatTimer;
    private final Timer geminiEmbeddingTimer;
    private final Timer staleJobTimer;
    private final Timer digestJobTimer;

    public MetricsService(MeterRegistry registry) {
        // Application lifecycle counters
        this.applicationsCreated = Counter.builder("jobtrackr.applications.created")
                .description("Total job applications created")
                .register(registry);

        this.statusChanges = Counter.builder("jobtrackr.applications.status_changes")
                .description("Total status transitions")
                .register(registry);

        // AI processing counters
        this.aiTagExtractionSuccess = Counter.builder("jobtrackr.ai.tag_extraction")
                .tag("result", "success")
                .description("Successful Gemini tag extractions")
                .register(registry);

        this.aiTagExtractionFailure = Counter.builder("jobtrackr.ai.tag_extraction")
                .tag("result", "failure")
                .description("Failed Gemini tag extractions")
                .register(registry);

        this.embeddingCacheHits = Counter.builder("jobtrackr.ai.embedding_cache")
                .tag("result", "hit")
                .description("Resume embedding cache hits")
                .register(registry);

        this.embeddingCacheMisses = Counter.builder("jobtrackr.ai.embedding_cache")
                .tag("result", "miss")
                .description("Resume embedding cache misses")
                .register(registry);

        // Notification counters
        this.emailsIngested = Counter.builder("jobtrackr.email.ingested")
                .description("Emails processed via ingestion endpoint")
                .register(registry);

        this.remindersSent = Counter.builder("jobtrackr.reminders.sent")
                .description("Reminders successfully delivered")
                .register(registry);

        this.remindersFailed = Counter.builder("jobtrackr.reminders.failed")
                .description("Reminders failed after max attempts")
                .register(registry);

        this.staleApplicationsFlagged = Counter.builder("jobtrackr.scheduler.stale_flagged")
                .description("Applications flagged as stale by the daily job")
                .register(registry);

        // Gemini API latency timers
        this.geminiChatTimer = Timer.builder("jobtrackr.gemini.chat.duration")
                .description("Gemini chat API call duration")
                .register(registry);

        this.geminiEmbeddingTimer = Timer.builder("jobtrackr.gemini.embedding.duration")
                .description("Gemini embedding API call duration")
                .register(registry);

        // Scheduler execution timers
        this.staleJobTimer = Timer.builder("jobtrackr.scheduler.stale_job.duration")
                .description("Stale detection job execution time")
                .register(registry);

        this.digestJobTimer = Timer.builder("jobtrackr.scheduler.digest_job.duration")
                .description("Weekly digest job execution time")
                .register(registry);
    }

    public void incrementApplicationsCreated() { applicationsCreated.increment(); }
    public void incrementStatusChanges()       { statusChanges.increment(); }

    public void incrementTagExtractionSuccess() { aiTagExtractionSuccess.increment(); }
    public void incrementTagExtractionFailure() { aiTagExtractionFailure.increment(); }

    public void incrementEmbeddingCacheHit()   { embeddingCacheHits.increment(); }
    public void incrementEmbeddingCacheMiss()  { embeddingCacheMisses.increment(); }

    public void incrementEmailsIngested()      { emailsIngested.increment(); }
    public void incrementRemindersSent()       { remindersSent.increment(); }
    public void incrementRemindersFailed()     { remindersFailed.increment(); }
    public void incrementStaleApplicationsFlagged() { staleApplicationsFlagged.increment(); }

    public void recordGeminiChatCall(long durationMs) {
        geminiChatTimer.record(durationMs, TimeUnit.MILLISECONDS);
    }

    public void recordGeminiEmbeddingCall(long durationMs) {
        geminiEmbeddingTimer.record(durationMs, TimeUnit.MILLISECONDS);
    }

    public Timer getStaleJobTimer()  { return staleJobTimer; }
    public Timer getDigestJobTimer() { return digestJobTimer; }
}