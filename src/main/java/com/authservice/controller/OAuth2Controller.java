package com.authservice.controller;

import com.authservice.dto.request.OAuth2CallbackRequest;
import com.authservice.security.TokenPair;
import com.authservice.service.OAuth2Service;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.view.RedirectView;

import java.io.IOException;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Slf4j
public class OAuth2Controller {

    private final OAuth2Service oauth2Service;

    /**
     * Initiate Google OAuth2 login
     */
    @GetMapping("/oauth2/login/google")
    public RedirectView initiateGoogleLogin(HttpServletRequest request) {
        log.info("Initiating Google OAuth2 login flow");
        String authorizationUrl = oauth2Service.generateAuthorizationUrl("google", request);
        return new RedirectView(authorizationUrl);
    }

    /**
     * Handle the OAuth2 callback from Angular frontend
     */
    @PostMapping("/oauth/callback")
    public ResponseEntity<?> handleCallbackFromClient(@RequestBody OAuth2CallbackRequest callbackRequest) {
        log.info("Handling OAuth2 callback from Angular client");
        
        try {
            TokenPair tokenPair = oauth2Service.exchangeCodeForTokens(
                    callbackRequest.getCode(),
                    callbackRequest.getCodeVerifier(),
                    callbackRequest.getRedirectUri(),
                    callbackRequest.getProvider());
            
            return ResponseEntity.ok(tokenPair);
        } catch (Exception e) {
            log.error("OAuth2 token exchange failed", e);
            return ResponseEntity.badRequest().body("Authentication failed: " + e.getMessage());
        }
    }
    
    /**
     * Refresh access token using refresh token
     */
    @PostMapping("/refresh-token")
    public ResponseEntity<?> refreshToken(@RequestParam String refreshToken) {
        try {
            TokenPair tokenPair = oauth2Service.refreshAccessToken(refreshToken);
            return ResponseEntity.ok(tokenPair);
        } catch (Exception e) {
            log.error("Token refresh failed", e);
            return ResponseEntity.badRequest().body("Token refresh failed: " + e.getMessage());
        }
    }
    
    /**
     * Log out user and invalidate the refresh token
     */
    @PostMapping("/logout")
    public ResponseEntity<?> logout(@RequestParam String refreshToken) {
        oauth2Service.revokeToken(refreshToken);
        return ResponseEntity.ok().body("Logged out successfully");
    }
}