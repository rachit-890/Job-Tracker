package com.proj.jobtracker.event;

/**
 * Central registry of all Kafka topic names.
 * Every producer and consumer imports from here — no hardcoded strings anywhere.
 *
 * DLT naming convention: <original-topic>.DLT for all topics.
 * Every main topic has a corresponding DLT bean defined in KafkaConfig.
 */
public final class KafkaTopics {

    // Main topics
    public static final String APPLICATION_CREATED = "jobtrackr.application.created";
    public static final String STATUS_CHANGED      = "jobtrackr.status.changed";
    public static final String RESUME_SCORED       = "jobtrackr.resume.scored";
    public static final String REMINDER_CREATED    = "jobtrackr.reminder.created";
    public static final String DIGEST_GENERATED    = "jobtrackr.digest.generated";

    // Dead-letter topics — must all have NewTopic beans in KafkaConfig
    // since auto-topic creation is disabled.
    public static final String APPLICATION_CREATED_DLT = APPLICATION_CREATED + ".DLT";
    public static final String STATUS_CHANGED_DLT      = STATUS_CHANGED + ".DLT";
    public static final String RESUME_SCORED_DLT       = RESUME_SCORED + ".DLT";
    public static final String REMINDER_CREATED_DLT    = REMINDER_CREATED + ".DLT";
    public static final String DIGEST_GENERATED_DLT    = DIGEST_GENERATED + ".DLT";

    private KafkaTopics() {
    }
}