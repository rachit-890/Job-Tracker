package com.rachit.jobtrackr.repository;

import com.rachit.jobtrackr.entity.ResumeEmbedding;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ResumeEmbeddingRepository extends JpaRepository<ResumeEmbedding, String> {
    //findById(resumeHash) is inherited — that's the cache lookup.
}