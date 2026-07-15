package com.rachit.jobtrackr.repository;

import com.rachit.jobtrackr.entity.ApplicationNote;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface ApplicationNoteRepository extends JpaRepository<ApplicationNote, UUID> {

    List<ApplicationNote> findByApplicationIdOrderByCreatedAtDesc(UUID applicationId);
}
