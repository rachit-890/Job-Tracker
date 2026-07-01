package com.proj.jobtracker.service;

import com.proj.jobtracker.dto.TokenResponse;
import com.proj.jobtracker.entity.User;
import com.proj.jobtracker.repository.UserRepository;
import com.proj.jobtracker.security.JwtService;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public AuthService(UserRepository userRepository, PasswordEncoder passwordEncoder, JwtService jwtService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
    }

    public TokenResponse login(String username, String rawPassword) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new BadCredentialsException("Invalid username or password"));

        if (!passwordEncoder.matches(rawPassword, user.getPasswordHash())) {
            throw new BadCredentialsException("Invalid username or password");
        }

        String accessToken = jwtService.generateAccessToken(user.getUsername());
        String refreshToken = jwtService.generateRefreshToken(user.getUsername());
        return TokenResponse.of(accessToken, refreshToken);
    }

    public TokenResponse refresh(String refreshToken) {
        if (!jwtService.isTokenValid(refreshToken, "refresh")) {
            throw new BadCredentialsException("Invalid or expired refresh token");
        }
        String username = jwtService.extractUsername(refreshToken);
        // Confirm the user still exists before issuing a new access token.
        userRepository.findByUsername(username)
                .orElseThrow(() -> new BadCredentialsException("Invalid refresh token"));

        String newAccessToken = jwtService.generateAccessToken(username);
        // Rotate the refresh token too, so a leaked refresh token has a shorter useful life.
        String newRefreshToken = jwtService.generateRefreshToken(username);
        return TokenResponse.of(newAccessToken, newRefreshToken);
    }
}
