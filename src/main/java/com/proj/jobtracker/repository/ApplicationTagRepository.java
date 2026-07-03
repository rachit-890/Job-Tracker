package com.proj.jobtracker.repository;

import com.proj.jobtracker.entity.ApplicationTag;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ApplicationTagRepository extends JpaRepository<ApplicationTag, UUID> {

    List<ApplicationTag> findByApplicationIdOrderByTagAsc(UUID applicationId);

    Optional<ApplicationTag> findByApplicationIdAndTag(UUID applicationId, String tag);

    boolean existsByApplicationIdAndTag(UUID applicationId, String tag);

    @Modifying
    @Query("delete from ApplicationTag t where t.applicationId = :appId and t.tag = :tag")
    int deleteByApplicationIdAndTag(@Param("appId") UUID applicationId, @Param("tag") String tag);

    @Modifying
    @Query("delete from ApplicationTag t where t.applicationId = :appId")
    void deleteAllByApplicationId(@Param("appId") UUID applicationId);
}
