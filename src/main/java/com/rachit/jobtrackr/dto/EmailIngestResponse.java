package com.rachit.jobtrackr.dto;

import java.util.UUID;

public record EmailIngestResponse(
        String status,          // PROCESSED, DUPLICATE, NO_MATCH
        String classification,  // REJECTION, INTERVIEW, OTHER
        UUID applicationId,
        String message
) {
}