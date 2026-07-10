package com.rachit.jobtrackr.repository;

import com.rachit.jobtrackr.entity.ApplicationTag;
import com.rachit.jobtrackr.entity.TagSource;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Repository
public interface ApplicationTagRepository extends JpaRepository<ApplicationTag, UUID> {

    List<ApplicationTag> findByApplicationId(UUID applicationId);

    @Transactional
    void deleteByApplicationId(UUID applicationId);

    // FIX: single bulk DELETE instead of N individual deleteById() calls.
    // Previously AiProcessingService iterated and called deleteById per tag
    // resulting in N round-trips to the DB for N tags.
    // This issues a single DELETE WHERE application_id = ? AND source = ?
    @Modifying
    @Transactional
    @Query("DELETE FROM ApplicationTag t WHERE t.applicationId = :applicationId AND t.source = :source")
    void deleteByApplicationIdAndSource(
            @Param("applicationId") UUID applicationId,
            @Param("source") TagSource source);
}