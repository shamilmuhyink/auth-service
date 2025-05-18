package com.authservice.service;

import com.authservice.security.TokenPair;
import jakarta.servlet.http.HttpServletRequest;

public interface OAuth2Service {
    
    /**
     * Generate an OAuth2 authorization URL for a specific provider
     */
    String generateAuthorizationUrl(String provider, HttpServletRequest request);
    
    /**
     * Exchange authorization code for tokens
     */
    TokenPair exchangeCodeForTokens(String code, String codeVerifier, String redirectUri, String provider);
    
    /**
     * Refresh access token using refresh token
     */
    TokenPair refreshAccessToken(String refreshToken);
    
    /**
     * Revoke token (logout)
     */
    void revokeToken(String refreshToken);
}