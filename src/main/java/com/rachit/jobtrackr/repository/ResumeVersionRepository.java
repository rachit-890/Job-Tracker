package com.rachit.jobtrackr.repository;

import com.rachit.jobtrackr.entity.ResumeVersion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ResumeVersionRepository extends JpaRepository<ResumeVersion, UUID> {

    Optional<ResumeVersion> findByLabel(String label);
    
    List<ResumeVersion> findAllByOrderByCreatedAtDesc();
}
