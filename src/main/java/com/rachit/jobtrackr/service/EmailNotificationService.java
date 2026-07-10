package com.rachit.jobtrackr.service;

import com.rachit.jobtrackr.entity.JobApplication;
import com.rachit.jobtrackr.entity.Reminder;
import com.rachit.jobtrackr.event.DigestGeneratedPayload;
import com.rachit.jobtrackr.event.StatusChangedPayload;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

/**
 * Email implementation of NotificationService.
 *
 * Two modes controlled by jobtrackr.notification.email-enabled:
 *   false (default) — structured log output only, no SMTP required.
 *   true            — real email via JavaMailSender.
 */
@Service
public class EmailNotificationService implements NotificationService {

    private static final Logger log = LoggerFactory.getLogger(EmailNotificationService.class);

    private final JavaMailSender mailSender;

    @Value("${jobtrackr.notification.email-enabled:false}")
    private boolean emailEnabled;

    @Value("${jobtrackr.notification.from-address:noreply@jobtrackr.dev}")
    private String fromAddress;

    public EmailNotificationService(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    @Override
    public void notifyStatusChange(StatusChangedPayload payload) {
        String subject = "JobTrackr — Status Update: %s at %s"
                .formatted(payload.role(), payload.company());
        String body = """
                Your application status has been updated.

                Company  : %s
                Role     : %s
                Status   : %s → %s

                Log in to JobTrackr to view the full timeline.
                """.formatted(
                payload.company(),
                payload.role(),
                payload.oldStatus(),
                payload.newStatus());

        send(subject, body);
    }

    @Override
    public void sendReminderNotification(Reminder reminder, JobApplication application) {
        String subject = "JobTrackr — Follow-up Reminder: %s at %s"
                .formatted(application.getRole(), application.getCompany());
        String body = """
                You applied to this role %d days ago and haven't heard back yet.

                Company      : %s
                Role         : %s
                Applied Date : %s

                Consider following up with the hiring team.
                Log in to JobTrackr to update the status once you hear back.
                """.formatted(
                ChronoUnit.DAYS.between(application.getAppliedDate(), LocalDate.now()),
                application.getCompany(),
                application.getRole(),
                application.getAppliedDate());

        send(subject, body);
    }

    @Override
    public void sendWeeklyDigest(DigestGeneratedPayload payload) {
        String subject = "JobTrackr — Weekly Job Search Digest (%s to %s)"
                .formatted(payload.weekStart(), payload.weekEnd());
        String body = """
                Here's your job search summary for the week.

                Period          : %s to %s
                New Applications: %d
                Status Changes  : %d
                Responses       : %d

                Log in to JobTrackr to view your full analytics dashboard.
                """.formatted(
                payload.weekStart(),
                payload.weekEnd(),
                payload.newApplications(),
                payload.statusChanges(),
                payload.responseCount());

        send(subject, body);
    }

    private void send(String subject, String body) {
        if (!emailEnabled) {
            log.info("[Notification] [STUB] Subject: {} | Body preview: {}",
                    subject, body.lines().findFirst().orElse(""));
            return;
        }

        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(fromAddress);
            message.setSubject(subject);
            message.setText(body);
            mailSender.send(message);
            log.info("[Notification] Email sent: {}", subject);
        } catch (Exception e) {
            log.error("[Notification] Failed to send email: {}", subject, e);
            throw e;
        }
    }
}