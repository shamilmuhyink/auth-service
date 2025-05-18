package com.authservice.service.implementations;

import com.authservice.exception.OAuth2AuthenticationProcessingException;
import com.authservice.exception.TokenRefreshException;
import com.authservice.model.AuthProvider;
import com.authservice.model.RefreshToken;
import com.authservice.model.User;
import com.authservice.repository.RefreshTokenRepository;
import com.authservice.repository.UserRepository;
import com.authservice.security.JwtTokenProvider;
import com.authservice.security.TokenPair;
import com.authservice.security.UserPrincipal;
import com.authservice.security.oauth2.OAuth2UserInfo;
import com.authservice.security.oauth2.OAuth2UserInfoFactory;
import com.authservice.service.OAuth2Service;
import com.authservice.service.RefreshTokenService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.codec.Base64;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.util.UriComponentsBuilder;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class OAuth2ServiceImpl implements OAuth2Service {

    private final JwtTokenProvider tokenProvider;
    private final RefreshTokenService refreshTokenService;
    private final UserRepository userRepository;
    private final ClientRegistrationRepository clientRegistrationRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final RestTemplate restTemplate = new RestTemplate();
    
    @Value("${app.oauth2.pkce.enabled:true}")
    private boolean pkceEnabled;

    private final Map<String, String> codeVerifierStore = new HashMap<>();

    @Override
    public String generateAuthorizationUrl(String provider, HttpServletRequest request) {
        ClientRegistration clientRegistration = clientRegistrationRepository.findByRegistrationId(provider);
        if (clientRegistration == null) {
            throw new OAuth2AuthenticationProcessingException("Unsupported OAuth2 provider: " + provider);
        }
        
        UriComponentsBuilder uriBuilder = UriComponentsBuilder
            .fromUriString(clientRegistration.getProviderDetails().getAuthorizationUri())
            .queryParam("client_id", clientRegistration.getClientId())
            .queryParam("redirect_uri", getRedirectUri(clientRegistration))
            .queryParam("response_type", "code")
            .queryParam("scope", String.join(" ", clientRegistration.getScopes()));
            
        // Add PKCE parameters if enabled
        if (pkceEnabled) {
            try {
                String codeVerifier = generateCodeVerifier();
                String codeChallenge = generateCodeChallenge(codeVerifier);
                
                // Store code verifier temporarily (in production, this should be in a session or cache)
                String sessionId = request.getSession().getId();
                codeVerifierStore.put(sessionId, codeVerifier);
                
                uriBuilder.queryParam("code_challenge", codeChallenge)
                          .queryParam("code_challenge_method", "S256");
                          
                log.debug("Added PKCE parameters to authorization request");
            } catch (NoSuchAlgorithmException e) {
                log.error("Failed to generate PKCE parameters", e);
            }
        }
        
        return uriBuilder.build().toUriString();
    }

    @Override
    @Transactional
    public TokenPair exchangeCodeForTokens(String code, String codeVerifier, String redirectUri, String provider) {
        // 1. Get the client registration for the provider
        ClientRegistration clientRegistration = clientRegistrationRepository.findByRegistrationId(provider);
        if (clientRegistration == null) {
            throw new OAuth2AuthenticationProcessingException("Unsupported OAuth2 provider: " + provider);
        }
        
        // If codeVerifier is not provided but PKCE is enabled, try to retrieve it from storage
        if ((codeVerifier == null || codeVerifier.isEmpty()) && pkceEnabled) {
            HttpServletRequest request = ((ServletRequestAttributes) RequestContextHolder.getRequestAttributes()).getRequest();
            String sessionId = request.getSession().getId();
            codeVerifier = codeVerifierStore.get(sessionId);
            
            // Remove the code verifier from storage after using it
            if (codeVerifier != null) {
                codeVerifierStore.remove(sessionId);
            }
        }
        
        // 2. Exchange code for tokens
        Map<String, Object> tokenResponse = exchangeAuthorizationCode(
                clientRegistration, code, codeVerifier, redirectUri);
        
        String accessTokenValue = (String) tokenResponse.get("access_token");
        if (accessTokenValue == null) {
            throw new OAuth2AuthenticationProcessingException("Failed to obtain access token");
        }
        
        // 3. Fetch user information using the access token
        Map<String, Object> userAttributes = getUserInfo(clientRegistration, accessTokenValue);
        
        // 4. Process user information
        OAuth2UserInfo userInfo = OAuth2UserInfoFactory.getOAuth2UserInfo(provider, userAttributes);
        
        if (userInfo.getEmail() == null || userInfo.getEmail().isEmpty()) {
            throw new OAuth2AuthenticationProcessingException("Email not found from OAuth2 provider");
        }
        
        // 5. Find or create user
        User user = findOrCreateUser(provider, userInfo);
        
        // 6. Update last login time
        user.setLastLoginAt(LocalDateTime.now());
        userRepository.save(user);
        
        // 7. Create UserPrincipal and generate tokens
        UserPrincipal userPrincipal = UserPrincipal.create(user);
        
        // 8. Set authentication in security context
        Authentication authentication = new UsernamePasswordAuthenticationToken(
                userPrincipal, null, userPrincipal.getAuthorities());
        SecurityContextHolder.getContext().setAuthentication(authentication);
        
        // 9. Generate JWT access token and refresh token
        String jwt = tokenProvider.generateAccessToken(userPrincipal);
        String refreshToken = refreshTokenService.createRefreshToken(user.getId());
        
        return new TokenPair(jwt, refreshToken);
    }
    
    @Override
    @Transactional
    public TokenPair refreshAccessToken(String refreshToken) {
        return refreshTokenService.findByToken(refreshToken)
            .map(refreshTokenService::verifyExpiration)
            .map(RefreshToken::getUser)
            .map(userId -> {
                User user = userRepository.findById(userId)
                    .orElseThrow(() -> new TokenRefreshException("User not found for refresh token"));
                
                UserPrincipal userPrincipal = UserPrincipal.create(user);
                String accessToken = tokenProvider.generateAccessToken(userPrincipal);
                
                return new TokenPair(accessToken, refreshToken);
            })
            .orElseThrow(() -> new TokenRefreshException("Invalid refresh token"));
    }
    
    @Override
    @Transactional
    public void revokeToken(String refreshToken) {
        refreshTokenService.deleteByToken(refreshToken);
    }
    
    private Map<String, Object> exchangeAuthorizationCode(
            ClientRegistration clientRegistration, 
            String code, 
            String codeVerifier, 
            String redirectUri) {
        
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        
        MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
        body.add("client_id", clientRegistration.getClientId());
        body.add("redirect_uri", redirectUri != null ? redirectUri : getRedirectUri(clientRegistration));
        body.add("grant_type", "authorization_code");
        body.add("code", code);
        body.add("client_secret", clientRegistration.getClientSecret());

        // Ensure code_verifier is always included when PKCE is enabled
        if (pkceEnabled) {
            if (codeVerifier != null && !codeVerifier.isEmpty()) {
                body.add("code_verifier", codeVerifier);
                log.debug("Added code_verifier to token request");
            } else {
                log.warn("PKCE is enabled but code_verifier is missing for token request");
            }
        }
        
        HttpEntity<MultiValueMap<String, String>> requestEntity = new HttpEntity<>(body, headers);
        
        ResponseEntity<Map> responseEntity = restTemplate.exchange(
                clientRegistration.getProviderDetails().getTokenUri(),
                HttpMethod.POST,
                requestEntity,
                Map.class);
        
        return responseEntity.getBody();
    }
    
    private Map<String, Object> getUserInfo(ClientRegistration clientRegistration, String accessToken) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(accessToken);
        
        HttpEntity<?> requestEntity = new HttpEntity<>(headers);
        
        ResponseEntity<Map> responseEntity = restTemplate.exchange(
                clientRegistration.getProviderDetails().getUserInfoEndpoint().getUri(),
                HttpMethod.GET,
                requestEntity,
                Map.class);
        
        return responseEntity.getBody();
    }
    
    @Transactional
    private User findOrCreateUser(String provider, OAuth2UserInfo userInfo) {
        Optional<User> userOptional = userRepository.findByEmail(userInfo.getEmail());
        
        if (userOptional.isPresent()) {
            User existingUser = userOptional.get();
            
            // If user exists but with different provider, handle appropriately
            if (!existingUser.getProvider().equals(provider)) {
                log.warn("User {} already exists with provider {}, but is trying to login with {}",
                        userInfo.getEmail(), existingUser.getProvider(), provider);
            }
            
            // Update user information
            existingUser.setName(userInfo.getName());
            existingUser.setProfileImageUrl(userInfo.getImageUrl());
            return userRepository.save(existingUser);
        } else {
            // Create a new user
            User newUser = new User();
            newUser.setProvider(AuthProvider.valueOf(provider));
            newUser.setProviderId(userInfo.getId());
            newUser.setName(userInfo.getName());
            newUser.setEmail(userInfo.getEmail());
            newUser.setEmailVerified(true);
            newUser.setProfileImageUrl(userInfo.getImageUrl());
            newUser.setCreatedAt(LocalDateTime.now());
            // Assign default roles here
            
            return userRepository.save(newUser);
        }
    }
    
    private String getRedirectUri(ClientRegistration clientRegistration) {
        // Don't modify the URI - use it exactly as configured in application.yml
        return clientRegistration.getRedirectUri();
    }
    
    private String generateCodeVerifier() {
        // PKCE spec recommends a minimum 32 bytes (256 bits) of entropy
        final int CODE_VERIFIER_BYTES = 32;
        
        SecureRandom secureRandom = new SecureRandom();
        byte[] codeVerifier = new byte[CODE_VERIFIER_BYTES];
        secureRandom.nextBytes(codeVerifier);
        
        // Encode as URL-safe Base64 string without padding
        return java.util.Base64.getUrlEncoder()
                .withoutPadding()
                .encodeToString(codeVerifier);
    }
    
    private String generateCodeChallenge(String codeVerifier) throws NoSuchAlgorithmException {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        byte[] bytes = digest.digest(codeVerifier.getBytes(StandardCharsets.US_ASCII));
        // Encode as URL-safe Base64 string without padding
        return java.util.Base64.getUrlEncoder()
                .withoutPadding()
                .encodeToString(bytes);
    }
}