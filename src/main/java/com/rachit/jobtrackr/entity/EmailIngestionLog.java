package com.rachit.jobtrackr.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "email_ingestion_log")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EmailIngestionLog {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "idempotency_key", nullable = false, unique = true)
    private String idempotencyKey;

    @Column(name = "application_id")
    private UUID applicationId;

    @Column(name = "classification")
    private String classification;

    @Column(name = "processed_at", nullable = false)
    private Instant processedAt;
}