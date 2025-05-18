package com.authservice.dto.request;

import lombok.Data;

@Data
public class OAuth2CallbackRequest {
    private String code;            // Authorization code from Google
    private String codeVerifier;    // PKCE code verifier
    private String redirectUri;     // Client redirect URI
    private String provider = "google";
}