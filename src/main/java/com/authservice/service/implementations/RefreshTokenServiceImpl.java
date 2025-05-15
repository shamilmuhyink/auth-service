package com.authservice.service.implementations;

import com.authservice.exception.TokenRefreshException;
import com.authservice.model.RefreshToken;
import com.authservice.repository.RefreshTokenRepository;
import com.authservice.service.RefreshTokenService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class RefreshTokenServiceImpl implements RefreshTokenService {
    
    @Value("${app.jwt.refresh-token.expiration}")
    private long refreshTokenDurationMs;
    
    private final RefreshTokenRepository refreshTokenRepository;
    
    public Optional<RefreshToken> findByToken(String token) {
        return refreshTokenRepository.findByToken(token);
    }
    
    @Transactional
    public String createRefreshToken(Long userId) {
        // Delete any existing refresh tokens for this user
        refreshTokenRepository.deleteByUser(userId);
        
        // Create a new refresh token
        RefreshToken refreshToken = RefreshToken.builder()
                .user(userId)
                .expiryDate(Instant.now().plusMillis(refreshTokenDurationMs))
                .token(UUID.randomUUID().toString())
                .build();
        
        refreshToken = refreshTokenRepository.save(refreshToken);
        return refreshToken.getToken();
    }
    
    /**
     * Verifies if the token is expired. If it's expired, delete it and throw an exception.
     * Otherwise, return the RefreshToken instance.
     */
    @Transactional
    public RefreshToken verifyExpiration(RefreshToken token) {
        if (token.getExpiryDate().compareTo(Instant.now()) < 0) {
            refreshTokenRepository.delete(token);
            throw new TokenRefreshException("Refresh token was expired. Please make a new signin request");
        }
        
        return token;
    }
    
    @Transactional
    public void deleteByToken(String token) {
        refreshTokenRepository.deleteByToken(token);
    }
}