package com.proj.jobtracker.repository;

import com.proj.jobtracker.entity.ProcessedEvent;
import com.proj.jobtracker.entity.ProcessedEventId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ProcessedEventRepository
        extends JpaRepository<ProcessedEvent, ProcessedEventId> {
}