package com.proj.jobtracker.event;


/**
 * Central registry of Kafka topic names.
 * Every producer and consumer imports from here — no hardcoded strings anywhere.
 */
public final class KafkaTopics {

    public static final String APPLICATION_CREATED  = "jobtrackr.application.created";
    public static final String STATUS_CHANGED       = "jobtrackr.status.changed";
    public static final String RESUME_SCORED        = "jobtrackr.resume.scored";
    public static final String REMINDER_CREATED     = "jobtrackr.reminder.created";
    public static final String DIGEST_GENERATED     = "jobtrackr.digest.generated";

    // Dead-letter topics. Spring appends ".DLT" by default — we define explicit
    // names here so they're predictable and monitorable.
    public static final String APPLICATION_CREATED_DLT  = "jobtrackr.application.created.dlt";
    public static final String STATUS_CHANGED_DLT        = "jobtrackr.status.changed.dlt";

    private KafkaTopics() {
    }
}
