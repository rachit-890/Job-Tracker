package com.proj.jobtracker.consumer;

import com.proj.jobtracker.domain.ApplicationStatus;
import com.proj.jobtracker.domain.ReminderStatus;
import com.proj.jobtracker.entity.Reminder;
import com.proj.jobtracker.event.EventEnvelope;
import com.proj.jobtracker.event.KafkaTopics;
import com.proj.jobtracker.event.ReminderCreatedPayload;
import com.proj.jobtracker.event.StatusChangedPayload;
import com.proj.jobtracker.repository.ReminderRepository;
import com.proj.jobtracker.service.EventPublisher;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

/**
 * Manages reminder lifecycle in response to status changes.
 *
 * When an application moves to APPLIED:
 *   → Schedule a reminder N days out.
 *
 * When an application moves past APPLIED (any forward progress):
 *   → Cancel any pending reminders — no need to follow up if they responded.
 *
 * After creating a reminder row, publishes a ReminderCreatedEvent so the
 * NotificationConsumer can handle the actual delivery when the time comes.
 * This decouples reminder scheduling from reminder delivery.
 */
@Component
public class ReminderConsumer {

    private static final Logger log = LoggerFactory.getLogger(ReminderConsumer.class);
    private static final String CONSUMER_GROUP = "reminder-consumer-group";

    private final IdempotencyGuard idempotencyGuard;
    private final ReminderRepository reminderRepository;
    private final EventPublisher eventPublisher;

    @Value("${jobtrackr.stale.default-threshold-days:14}")
    private int reminderDays;

    public ReminderConsumer(IdempotencyGuard idempotencyGuard,
                            ReminderRepository reminderRepository,
                            EventPublisher eventPublisher) {
        this.idempotencyGuard = idempotencyGuard;
        this.reminderRepository = reminderRepository;
        this.eventPublisher = eventPublisher;
    }

    @KafkaListener(
            topics = KafkaTopics.STATUS_CHANGED,
            groupId = CONSUMER_GROUP
    )
    @Transactional
    public void onStatusChanged(ConsumerRecord<String, EventEnvelope<StatusChangedPayload>> record,
                                Acknowledgment ack) {
        EventEnvelope<StatusChangedPayload> envelope = record.value();

        if (idempotencyGuard.alreadyProcessed(CONSUMER_GROUP, envelope.eventId())) {
            ack.acknowledge();
            return;
        }

        StatusChangedPayload payload = envelope.payload();
        log.debug("[Reminder] Processing StatusChangedEvent: applicationId={} {} -> {}",
                payload.applicationId(), payload.oldStatus(), payload.newStatus());

        if (payload.newStatus() == ApplicationStatus.APPLIED) {
            scheduleReminder(payload);
        } else {
            cancelPendingReminders(payload);
        }

        ack.acknowledge();
    }

    private void scheduleReminder(StatusChangedPayload payload) {
        Instant remindAt = Instant.now().plus(reminderDays, ChronoUnit.DAYS);

        Reminder reminder = Reminder.builder()
                .applicationId(payload.applicationId())
                .remindAt(remindAt)
                .status(ReminderStatus.PENDING)
                .attemptCount(0)
                .build();

        Reminder saved = reminderRepository.save(reminder);
        log.info("[Reminder] Scheduled reminder: applicationId={} remindAt={}", payload.applicationId(), remindAt);

        eventPublisher.publishReminderCreated(new ReminderCreatedPayload(
                saved.getId(),
                payload.applicationId(),
                payload.company(),
                payload.role(),
                remindAt
        ));
    }

    private void cancelPendingReminders(StatusChangedPayload payload) {
        List<Reminder> pending = reminderRepository.findByApplicationIdAndStatus(
                payload.applicationId(), ReminderStatus.PENDING);

        pending.forEach(r -> r.setStatus(ReminderStatus.CANCELLED));
        reminderRepository.saveAll(pending);

        if (!pending.isEmpty()) {
            log.info("[Reminder] Cancelled {} pending reminder(s) for applicationId={}",
                    pending.size(), payload.applicationId());
        }
    }
}
