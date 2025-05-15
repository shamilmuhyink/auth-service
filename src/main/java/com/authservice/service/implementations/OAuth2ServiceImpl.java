package com.authservice.service.implementations;

import com.authservice.dto.response.AuthResponse;
import com.authservice.exception.BadRequestException;
import com.authservice.model.AuthProvider;
import com.authservice.model.Role;
import com.authservice.model.User;
import com.authservice.repository.RoleRepository;
import com.authservice.repository.UserRepository;
import com.authservice.security.JwtTokenProvider;
import com.authservice.security.UserPrincipal;
import com.authservice.security.oauth2.OAuth2UserInfo;
import com.authservice.security.oauth2.OAuth2UserInfoFactory;
import com.authservice.service.OAuth2Service;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.Map;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class OAuth2ServiceImpl implements OAuth2Service {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final JwtTokenProvider tokenProvider;

    /**
     * Process OAuth2 authentication and return JWT tokens
     *
     * @param authentication The OAuth2 authentication object
     * @return AuthResponse containing access and refresh tokens
     */
    public AuthResponse processOAuth2Authentication(Authentication authentication) {
        // Get the UserPrincipal from the authentication
        UserPrincipal userPrincipal = (UserPrincipal) authentication.getPrincipal();
        
        // Generate tokens using the appropriate methods
        String accessToken = tokenProvider.generateAccessToken(userPrincipal);
        String refreshToken = tokenProvider.generateRefreshToken(userPrincipal);
        
        // Calculate token expiration time in seconds
        long expiresInSeconds = tokenProvider.getAccessTokenExpirationMs() / 1000;
        
        // Create and return the AuthResponse using the builder pattern
        return AuthResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .tokenType("Bearer")
                .expiresIn(expiresInSeconds)
                .build();
    }

    /**
     * Process user registration or update for OAuth2 users
     *
     * @param registrationId The OAuth2 provider ID
     * @param oAuth2UserInfo The OAuth2 user information
     * @return The created or updated user
     */
    private User processUserRegistration(String registrationId, OAuth2UserInfo oAuth2UserInfo) {
        Optional<User> userOptional = userRepository.findByEmail(oAuth2UserInfo.getEmail());
        User user;

        if (userOptional.isPresent()) {
            user = userOptional.get();

            // Update existing user with OAuth2 provider info if not already set
            if (user.getProvider() == null || !user.getProvider().equals(registrationId)) {
                user.setProvider(AuthProvider.valueOf(registrationId));
                user.setProviderId(oAuth2UserInfo.getId());
                user = userRepository.save(user);
                log.info("Updated existing user with OAuth2 provider info: {}", user.getEmail());
            }
        } else {
            // Create new user from OAuth2 info
            user = createNewUser(registrationId, oAuth2UserInfo);
            log.info("Created new user from OAuth2 data: {}", user.getEmail());
        }

        return user;
    }

    /**
     * Create a new user from OAuth2 user information
     *
     * @param registrationId The OAuth2 provider ID
     * @param oAuth2UserInfo The OAuth2 user information
     * @return The newly created user
     */
    private User createNewUser(String registrationId, OAuth2UserInfo oAuth2UserInfo) {
        User user = new User();
        user.setName(oAuth2UserInfo.getName());
        user.setEmail(oAuth2UserInfo.getEmail());
        user.setUsername(oAuth2UserInfo.getEmail()); // Use email as username for OAuth2 users
        user.setProvider(AuthProvider.valueOf(registrationId));
        user.setProviderId(oAuth2UserInfo.getId());

        // Set default USER role
        Role userRole = roleRepository.findByName(Role.RoleName.ROLE_USER)
                .orElseThrow(() -> {
                    log.error("User role not found in the database");
                    return new RuntimeException("User Role not set.");
                });
        user.setRoles(Collections.singleton(userRole));

        return userRepository.save(user);
    }
}