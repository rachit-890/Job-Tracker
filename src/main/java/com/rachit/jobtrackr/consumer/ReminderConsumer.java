package com.rachit.jobtrackr.consumer;

import com.rachit.jobtrackr.domain.ApplicationStatus;
import com.rachit.jobtrackr.domain.ReminderStatus;
import com.rachit.jobtrackr.entity.Reminder;
import com.rachit.jobtrackr.event.EventEnvelope;
import com.rachit.jobtrackr.event.KafkaTopics;
import com.rachit.jobtrackr.event.ReminderCreatedPayload;
import com.rachit.jobtrackr.event.StatusChangedPayload;
import com.rachit.jobtrackr.repository.ReminderRepository;
import com.rachit.jobtrackr.service.EventPublisher;
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

@Component
public class ReminderConsumer {

    private static final Logger log = LoggerFactory.getLogger(ReminderConsumer.class);

    @Value("${jobtrackr.kafka.consumer-groups.reminder}")
    private String consumerGroup;

    @Value("${jobtrackr.stale.default-threshold-days:14}")
    private int reminderDays;

    private final IdempotencyGuard idempotencyGuard;
    private final ReminderRepository reminderRepository;
    private final EventPublisher eventPublisher;

    public ReminderConsumer(IdempotencyGuard idempotencyGuard,
                             ReminderRepository reminderRepository,
                             EventPublisher eventPublisher) {
        this.idempotencyGuard = idempotencyGuard;
        this.reminderRepository = reminderRepository;
        this.eventPublisher = eventPublisher;
    }

    @KafkaListener(
            topics = KafkaTopics.STATUS_CHANGED,
            groupId = "${jobtrackr.kafka.consumer-groups.reminder}"
    )
    @Transactional
    public void onStatusChanged(
            ConsumerRecord<String, EventEnvelope<StatusChangedPayload>> record,
            Acknowledgment ack) {

        EventEnvelope<StatusChangedPayload> envelope = record.value();

        if (idempotencyGuard.alreadyProcessed(consumerGroup, envelope.eventId())) {
            ack.acknowledge();
            return;
        }

        StatusChangedPayload payload = envelope.payload();
        log.info("[Reminder] Processing StatusChangedEvent: eventId={} applicationId={} {} -> {}",
                envelope.eventId(), payload.applicationId(), payload.oldStatus(), payload.newStatus());

        if (payload.newStatus() == ApplicationStatus.APPLIED) {
            scheduleReminder(payload);
        } else {
            cancelPendingReminders(payload);
        }

        ack.acknowledge();
        log.info("[Reminder] Successfully processed eventId={} consumerGroup={}",
                envelope.eventId(), consumerGroup);
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
        log.info("[Reminder] Scheduled: applicationId={} remindAt={}",
                payload.applicationId(), remindAt);

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
            log.info("[Reminder] Cancelled {} reminder(s) for applicationId={}",
                    pending.size(), payload.applicationId());
        }
    }
}