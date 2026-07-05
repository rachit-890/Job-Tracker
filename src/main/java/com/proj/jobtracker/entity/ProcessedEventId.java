package com.proj.jobtracker.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.UUID;

@Embeddable
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ProcessedEventId implements Serializable {

    @Column(name = "consumer_group", nullable = false)
    private String consumerGroup;

    @Column(name = "event_id", nullable = false)
    private UUID eventId;
}
