package com.rachit.jobtrackr.repository;

import com.rachit.jobtrackr.entity.ApplicationGoal;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface GoalRepository extends JpaRepository<ApplicationGoal, UUID> {

    Optional<ApplicationGoal> findByActiveTrue();

    @Modifying
    @Query("UPDATE ApplicationGoal g SET g.active = false WHERE g.active = true")
    void deactivateAll();
}
