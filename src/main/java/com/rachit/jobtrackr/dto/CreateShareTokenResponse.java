package com.rachit.jobtrackr.dto;


import java.time.Instant;
import java.util.UUID;

public record CreateShareTokenResponse(
        UUID id,
        String token,
        Instant expiresAt,
        String shareUrl
) {
}
