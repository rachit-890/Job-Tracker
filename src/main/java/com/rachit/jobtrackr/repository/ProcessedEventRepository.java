package com.rachit.jobtrackr.repository;

import com.rachit.jobtrackr.entity.ProcessedEvent;
import com.rachit.jobtrackr.entity.ProcessedEventId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface ProcessedEventRepository
        extends JpaRepository<ProcessedEvent, ProcessedEventId> {

    boolean existsByIdConsumerGroupAndIdEventId(String consumerGroup, UUID eventId);
}