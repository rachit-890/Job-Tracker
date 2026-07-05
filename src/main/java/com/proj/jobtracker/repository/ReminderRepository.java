package com.proj.jobtracker.repository;


import com.proj.jobtracker.domain.ReminderStatus;
import com.proj.jobtracker.entity.Reminder;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Repository
public interface ReminderRepository extends JpaRepository<Reminder, UUID> {

    List<Reminder> findByApplicationIdAndStatus(UUID applicationId, ReminderStatus status);

    Page<Reminder> findByRemindAtBeforeAndStatus(Instant cutoff, ReminderStatus status, Pageable pageable);
}