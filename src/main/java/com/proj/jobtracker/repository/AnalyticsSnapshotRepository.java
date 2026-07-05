package com.proj.jobtracker.repository;


import com.proj.jobtracker.entity.AnalyticsSnapshot;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface AnalyticsSnapshotRepository extends JpaRepository<AnalyticsSnapshot, UUID> {
}
