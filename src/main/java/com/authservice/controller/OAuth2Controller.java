package com.authservice.controller;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.view.RedirectView;

import java.io.IOException;

@RestController
@RequestMapping("/api/auth/oauth2")
@RequiredArgsConstructor
@Slf4j
public class OAuth2Controller {

    private final ClientRegistrationRepository clientRegistrationRepository;

    /**
     * Initiates the OAuth2 login flow for the specified provider
     * 
     * @param provider The OAuth2 provider (e.g., "google")
     * @param request The HTTP request
     * @param response The HTTP response
     * @return A redirect to the OAuth2 authorization endpoint
     * @throws IOException If an error occurs during the redirect
     */
    @GetMapping("/login/{provider}")
    public RedirectView initiateOAuth2Login(
            @PathVariable String provider,
            HttpServletRequest request,
            HttpServletResponse response) throws IOException {
        
        log.info("Initiating OAuth2 login for provider: {}", provider);
        
        ClientRegistration clientRegistration = clientRegistrationRepository.findByRegistrationId(provider);
        if (clientRegistration == null) {
            log.error("OAuth2 provider not supported: {}", provider);
            response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Unsupported OAuth2 provider");
            return null;
        }
        
        // Build the OAuth2 authorization URL
        String authorizationRequestBaseUri = "/oauth2/authorization";
        String authorizationRequestUri = authorizationRequestBaseUri + "/" + provider;
        
        log.info("Redirecting to OAuth2 authorization endpoint for provider: {}", provider);
        return new RedirectView(authorizationRequestUri);
    }
}