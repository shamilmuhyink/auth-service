package com.authservice.service;

import com.authservice.model.RefreshToken;

import java.util.Optional;

public interface RefreshTokenService {
    public Optional<RefreshToken> findByToken(String token);
    public String createRefreshToken(Long userId);
    public RefreshToken verifyExpiration(RefreshToken token);
    public void deleteByToken(String token);
}
