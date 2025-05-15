package com.authservice.security;

import com.authservice.model.RefreshToken;
import com.authservice.repository.RefreshTokenRepository;
import io.jsonwebtoken.*;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import java.security.Key;
import java.time.Instant;
import java.util.Date;
import java.util.stream.Collectors;

@Service
@Slf4j
public class JwtTokenProvider {

    private final Key signingKey;
    private final long accessTokenExpirationMs;
    private final long refreshTokenExpirationMs;
    private final RefreshTokenRepository refreshTokenRepository;

    public JwtTokenProvider(
            @Value("${app.jwt.secret}") String jwtSecret,
            @Value("${app.jwt.access-token.expiration}") long accessTokenExpirationMs,
            @Value("${app.jwt.refresh-token.expiration}") long refreshTokenExpirationMs,
            RefreshTokenRepository refreshTokenRepository) {

        byte[] keyBytes = Decoders.BASE64.decode(jwtSecret);
        this.signingKey = Keys.hmacShaKeyFor(keyBytes);
        this.accessTokenExpirationMs = accessTokenExpirationMs;
        this.refreshTokenExpirationMs = refreshTokenExpirationMs;
        this.refreshTokenRepository = refreshTokenRepository;
    }

    /**
     * Generate token pair from authentication
     */
    public TokenPair generateTokenPair(Authentication authentication) {
        UserPrincipal userPrincipal = (UserPrincipal) authentication.getPrincipal();

        String accessToken = generateAccessToken(userPrincipal);
        String refreshToken = generateRefreshToken(userPrincipal);

        return new TokenPair(accessToken, refreshToken);
    }

    /**
     * Generate an access token for a user principal
     */
    public String generateAccessToken(UserPrincipal userPrincipal) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + accessTokenExpirationMs);

        return Jwts.builder()
                .setSubject(userPrincipal.getUsername())
                .setIssuedAt(now)
                .setExpiration(expiryDate)
                .claim("roles", userPrincipal.getAuthorities().stream()
                        .map(GrantedAuthority::getAuthority)
                        .collect(Collectors.toList()))
                .claim("userId", userPrincipal.getId())
                .signWith(signingKey)
                .compact();
    }

    /**
     * Generate a refresh token for a user and store it in the database
     */
    public String generateRefreshToken(UserPrincipal userPrincipal) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + refreshTokenExpirationMs);

        String tokenValue = Jwts.builder()
                .setSubject(userPrincipal.getUsername())
                .setIssuedAt(now)
                .setExpiration(expiryDate)
                .signWith(signingKey)
                .compact();

        // Store the refresh token in the database with token rotation
        RefreshToken refreshToken = new RefreshToken();
        refreshToken.setUser(userPrincipal.getId());
        refreshToken.setToken(tokenValue);
        refreshToken.setExpiryDate(Instant.now().plusMillis(refreshTokenExpirationMs));

        // Invalidate previous refresh tokens for this user (token rotation)
        refreshTokenRepository.deleteByUser(userPrincipal.getId());
        refreshTokenRepository.save(refreshToken);

        return tokenValue;
    }

    /**
     * Validate token with user details for use in JwtAuthenticationFilter
     */
    public boolean validateToken(String token, UserDetails userDetails) {
        try {
            Claims claims = Jwts.parserBuilder()
                    .setSigningKey(signingKey)
                    .build()
                    .parseClaimsJws(token)
                    .getBody();

            String username = claims.getSubject();
            Date expiration = claims.getExpiration();

            return (username.equals(userDetails.getUsername()) &&
                   !expiration.before(new Date()));
        } catch (Exception e) {
            logTokenValidationFailure(e);
            return false;
        }
    }
    
    /**
     * Extract username from token
     */
    public String getUsernameFromToken(String token) {
        try {
            return Jwts.parserBuilder()
                    .setSigningKey(signingKey)
                    .build()
                    .parseClaimsJws(token)
                    .getBody()
                    .getSubject();
        } catch (ExpiredJwtException e) {
            // Even if the token is expired, we can extract the username
            return e.getClaims().getSubject();
        } catch (Exception e) {
            log.error("Error extracting username from token", e);
            return null;
        }
    }
    
    /**
     * Extract user ID from a token
     */
    public Long getUserIdFromToken(String token) {
        try {
            return Jwts.parserBuilder()
                    .setSigningKey(signingKey)
                    .build()
                    .parseClaimsJws(token)
                    .getBody()
                    .get("userId", Long.class);
        } catch (Exception e) {
            log.error("Error extracting user ID from token", e);
            return null;
        }
    }
    
    /**
     * Log token validation failures with appropriate level
     */
    private void logTokenValidationFailure(Exception e) {
        if (e instanceof ExpiredJwtException) {
            log.debug("Expired JWT token: {}", e.getMessage());
        } else if (e instanceof MalformedJwtException) {
            log.warn("Invalid JWT token: {}", e.getMessage());
        } else if (e instanceof UnsupportedJwtException) {
            log.warn("Unsupported JWT token: {}", e.getMessage());
        } else if (e instanceof IllegalArgumentException) {
            log.warn("JWT claims string is empty: {}", e.getMessage());
        } else {
            log.error("JWT token validation error", e);
        }
    }

    public long getAccessTokenExpirationMs() {
        return accessTokenExpirationMs;
    }
}