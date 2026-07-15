package com.rachit.jobtrackr.repository;

import com.rachit.jobtrackr.entity.OutboxEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Repository
public interface OutboxRepository extends JpaRepository<OutboxEvent, UUID> {

    /**
     * Fetch unpublished events ordered by creation time (FIFO).
     * Uses the partial index on (created_at) WHERE published = FALSE.
     * Limit to a batch size to avoid loading the whole table.
     */
    @Query("SELECT o FROM OutboxEvent o WHERE o.published = false ORDER BY o.createdAt ASC LIMIT :limit")
    List<OutboxEvent> findUnpublishedBatch(@Param("limit") int limit);

    /**
     * Cleanup: delete published events older than the given cutoff.
     * Keeps the table small while retaining recent events for debugging.
     */
    @Modifying
    @Query("DELETE FROM OutboxEvent o WHERE o.published = true AND o.publishedAt < :cutoff")
    int deletePublishedBefore(@Param("cutoff") Instant cutoff);
}
