package com.rachit.jobtrackr.repository;

import com.rachit.jobtrackr.entity.ShareToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ShareTokenRepository extends JpaRepository<ShareToken, UUID> {

    Optional<ShareToken> findByToken(String token);

    Optional<ShareToken> findByTokenAndRevokedFalseAndExpiresAtAfter(String token, Instant now);
}
