package com.rachit.jobtrackr.service;

import com.rachit.jobtrackr.entity.ShareToken;
import com.rachit.jobtrackr.exception.ResourceNotFoundException;
import com.rachit.jobtrackr.repository.ShareTokenRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

@Service
public class ShareTokenService {

    private static final int TOKEN_VALIDITY_DAYS = 30;

    private final ShareTokenRepository shareTokenRepository;

    public ShareTokenService(ShareTokenRepository shareTokenRepository) {
        this.shareTokenRepository = shareTokenRepository;
    }

    @Transactional
    public ShareToken generate() {
        ShareToken token = ShareToken.builder()
                .token(UUID.randomUUID().toString().replace("-", ""))
                .expiresAt(Instant.now().plus(TOKEN_VALIDITY_DAYS, ChronoUnit.DAYS))
                .revoked(false)
                .build();
        return shareTokenRepository.save(token);
    }

    @Transactional
    public void revoke(String token) {
        ShareToken shareToken = shareTokenRepository.findByToken(token)
                .orElseThrow(() -> new ResourceNotFoundException("Share token not found"));
        shareToken.setRevoked(true);
        shareTokenRepository.save(shareToken);
    }

    public ShareToken validateAndGet(String token) {
        return shareTokenRepository
                .findByTokenAndRevokedFalseAndExpiresAtAfter(token, Instant.now())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Share token is invalid or has expired"));
    }
}
