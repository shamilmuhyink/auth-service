package com.authservice.service;

import com.authservice.dto.response.AuthResponse;
import org.springframework.security.core.Authentication;

public interface OAuth2Service {
    public AuthResponse processOAuth2Authentication(Authentication authentication);

}
