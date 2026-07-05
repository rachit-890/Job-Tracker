package com.proj.jobtracker.repository;

import com.proj.jobtracker.entity.ApplicationTag;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

public interface ApplicationTagRepository extends JpaRepository<ApplicationTag, UUID> {

    List<ApplicationTag> findByApplicationId(UUID applicationId);

    @Transactional
    void deleteByApplicationId(UUID applicationId);
}