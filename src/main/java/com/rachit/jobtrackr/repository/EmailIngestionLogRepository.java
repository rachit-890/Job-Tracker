package com.rachit.jobtrackr.repository;

import com.rachit.jobtrackr.entity.EmailIngestionLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface EmailIngestionLogRepository extends JpaRepository<EmailIngestionLog, UUID> {

    Optional<EmailIngestionLog> findByIdempotencyKey(String idempotencyKey);

    boolean existsByIdempotencyKey(String idempotencyKey);
}