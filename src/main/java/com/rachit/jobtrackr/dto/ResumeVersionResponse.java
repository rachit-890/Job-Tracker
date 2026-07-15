package com.rachit.jobtrackr.dto;

import com.rachit.jobtrackr.entity.ResumeVersion;

import java.time.Instant;
import java.util.UUID;

public record ResumeVersionResponse(
        UUID id,
        String label,
        String content,
        Instant createdAt
) {
    public static ResumeVersionResponse from(ResumeVersion version) {
        return new ResumeVersionResponse(
                version.getId(),
                version.getLabel(),
                version.getContent(),
                version.getCreatedAt()
        );
    }
}
