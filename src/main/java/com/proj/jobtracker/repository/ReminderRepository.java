package com.proj.jobtracker.repository;


import com.proj.jobtracker.entity.Reminder;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public interface ReminderRepository extends JpaRepository<Reminder, UUID> {

    List<Reminder> findByApplicationIdOrderByRemindAtAsc(UUID applicationId);

    // Used by the scheduler in Phase 5 to find reminders due for delivery
    @Query("""
            select r from Reminder r
            where r.status = 'PENDING'
              and r.remindAt <= :now
            order by r.remindAt asc
            """)
    List<Reminder> findDueReminders(@Param("now") Instant now);

    // Used by the reminder consumer in Phase 3 to cancel pending reminders
    // when an application moves past APPLIED status
    @Modifying
    @Query("""
            update Reminder r
            set r.status = com.proj.jobtracker.domain.ReminderStatus.CANCELLED,
                r.updatedAt = :now
            where r.applicationId = :appId
              and r.status = com.proj.jobtracker.domain.ReminderStatus.PENDING
            """)
    int cancelPendingRemindersForApplication(@Param("appId") UUID applicationId,
                                             @Param("now") Instant now);
}
