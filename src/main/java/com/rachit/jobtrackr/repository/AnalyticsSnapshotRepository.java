package com.rachit.jobtrackr.repository;


import com.rachit.jobtrackr.entity.AnalyticsSnapshot;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface AnalyticsSnapshotRepository extends JpaRepository<AnalyticsSnapshot, UUID> {
}
