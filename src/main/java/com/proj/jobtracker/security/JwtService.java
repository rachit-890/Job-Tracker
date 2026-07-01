package com.proj.jobtracker.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.function.Function;

@Component
public class JwtService {

    private final SecretKey signingKey;
    private final long accessTokenExpiryMs;
    private final long refreshTokenExpiryMs;

    public JwtService(
            @Value("${jobtrackr.jwt.secret}") String secret,
            @Value("${jobtrackr.jwt.access-expiry-minutes}") long accessExpiryMinutes,
            @Value("${jobtrackr.jwt.refresh-expiry-days}") long refreshExpiryDays) {
        this.signingKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.accessTokenExpiryMs = accessExpiryMinutes * 60 * 1000;
        this.refreshTokenExpiryMs = refreshExpiryDays * 24 * 60 * 60 * 1000;
    }

    public String generateAccessToken(String username) {
        return buildToken(username, accessTokenExpiryMs, "access");
    }

    public String generateRefreshToken(String username) {
        return buildToken(username, refreshTokenExpiryMs, "refresh");
    }

    private String buildToken(String username, long expiryMs, String tokenType) {
        Date now = new Date();
        return Jwts.builder()
                .subject(username)
                .claim("type", tokenType)
                .issuedAt(now)
                .expiration(new Date(now.getTime() + expiryMs))
                .signWith(signingKey)
                .compact();
    }

    public String extractUsername(String token) {
        return extractClaim(token, Claims::getSubject);
    }

    public String extractTokenType(String token) {
        return extractClaim(token, claims -> claims.get("type", String.class));
    }

    public boolean isTokenValid(String token, String expectedType) {
        try {
            Claims claims = extractAllClaims(token);
            boolean notExpired = claims.getExpiration().after(new Date());
            boolean correctType = expectedType.equals(claims.get("type", String.class));
            return notExpired && correctType;
        } catch (Exception e) {
            return false;
        }
    }

    private <T> T extractClaim(String token, Function<Claims, T> resolver) {
        return resolver.apply(extractAllClaims(token));
    }

    private Claims extractAllClaims(String token) {
        return Jwts.parser()
                .verifyWith(signingKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
}
