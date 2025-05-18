package com.authservice.controller;

import com.authservice.dto.response.UserResponse;
import com.authservice.security.UserPrincipal;
import com.authservice.dto.request.LoginRequest;
import com.authservice.dto.request.LogoutRequest;
import com.authservice.dto.request.SignUpRequest;
import com.authservice.dto.request.TokenRefreshRequest;
import com.authservice.dto.response.AuthResponse;
import com.authservice.dto.response.MessageResponse;
import com.authservice.model.AuthProvider;
import com.authservice.model.RefreshToken;
import com.authservice.model.Role;
import com.authservice.model.User;
import com.authservice.exception.AppException;
import com.authservice.exception.BadRequestException;
import com.authservice.exception.TokenRefreshException;
import com.authservice.repository.RoleRepository;
import com.authservice.repository.UserRepository;
import com.authservice.security.JwtTokenProvider;
import com.authservice.security.TokenPair;
import com.authservice.service.RefreshTokenService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Async;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.time.LocalDateTime;
import java.util.Set;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Authentication", description = "Authentication API")
public class AuthController {

    private final AuthenticationManager authenticationManager;
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider tokenProvider;
    private final RefreshTokenService refreshTokenService;
    
    @PostMapping("/login")
    @Operation(summary = "Login with username/password")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest loginRequest) {
        // Authenticate the user
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        loginRequest.getUsername(),
                        loginRequest.getPassword()
                )
        );
        
        SecurityContextHolder.getContext().setAuthentication(authentication);
        
        // Generate access and refresh tokens
        TokenPair tokenPair = tokenProvider.generateTokenPair(authentication);
        
        // Update last login time asynchronously to improve response time
        UserPrincipal userPrincipal = (UserPrincipal) authentication.getPrincipal();
        updateLastLoginTime(userPrincipal.getId());
        
        // Calculate token expiration time in seconds
        long expiresInSeconds = tokenProvider.getAccessTokenExpirationMs() / 1000;
        
        // Create response using builder pattern
        AuthResponse authResponse = AuthResponse.builder()
                .accessToken(tokenPair.getAccessToken())
                .refreshToken(tokenPair.getRefreshToken())
                .tokenType("Bearer")
                .expiresIn(expiresInSeconds)
                .build();
        
        return ResponseEntity.ok(authResponse);
    }

    /**
     * Update the user's last login time asynchronously
     */
    @Async
    private void updateLastLoginTime(Long userId) {
        userRepository.findById(userId)
                .ifPresent(user -> {
                    user.setLastLoginAt(LocalDateTime.now());
                    userRepository.save(user);
                });
    }
    
    @PostMapping("/register")
    @Operation(summary = "Register a new user")
    public ResponseEntity<UserResponse> register(@Valid @RequestBody SignUpRequest signupRequest) {
        // Check if username is already taken
        if (userRepository.existsByUsername(signupRequest.getUsername())) {
            throw new BadRequestException("Username is already taken");
        }
        
        // Check if email is already in use
        if (userRepository.existsByEmail(signupRequest.getEmail())) {
            throw new BadRequestException("Email is already in use");
        }
        
        // Create new user
        User user = User.builder()
                .username(signupRequest.getUsername())
                .email(signupRequest.getEmail())
                .name(signupRequest.getName())
                .password(passwordEncoder.encode(signupRequest.getPassword()))
                .provider(AuthProvider.LOCAL)
                .enabled(true)
                .build();
        
        // Assign default role
        Role userRole = roleRepository.findByName(Role.RoleName.ROLE_USER)
                .orElseThrow(() -> new AppException("Default role not found"));
        user.setRoles(Set.of(userRole));
        
        User savedUser = userRepository.save(user);
        
        URI location = ServletUriComponentsBuilder
                .fromCurrentContextPath().path("/api/users/{id}")
                .buildAndExpand(savedUser.getId()).toUri();
        
        return ResponseEntity.created(location)
                .body(UserResponse.fromUser(savedUser));
    }
    
    @PostMapping("/refresh")
    @Operation(summary = "Refresh access token")
    public ResponseEntity<AuthResponse> refreshToken(@Valid @RequestBody TokenRefreshRequest request) {
        return refreshTokenService.findByToken(request.getRefreshToken())
                .map(refreshTokenService::verifyExpiration)
                .map(RefreshToken::getUser)
                .map(userId -> {
                    User user = userRepository.findById(userId)
                            .orElseThrow(() -> new TokenRefreshException("User not found for refresh token"));
                
                    UserPrincipal userPrincipal = UserPrincipal.create(user);
                    String accessToken = tokenProvider.generateAccessToken(userPrincipal);
                
                    // Calculate token expiration time in seconds
                    long expiresInSeconds = tokenProvider.getAccessTokenExpirationMs() / 1000;
                
                    // Generate a new refresh token (token rotation)
                    String newRefreshToken = refreshTokenService.createRefreshToken(userId);
                
                    // Create response using builder pattern
                    AuthResponse authResponse = AuthResponse.builder()
                            .accessToken(accessToken)
                            .refreshToken(newRefreshToken)
                            .tokenType("Bearer")
                            .expiresIn(expiresInSeconds)
                            .build();
                
                    return ResponseEntity.ok(authResponse);
                })
                .orElseThrow(() -> new TokenRefreshException("Invalid refresh token"));
    }
    
//    @PostMapping("/logout")
//    @Operation(summary = "Log out a user")
//    public ResponseEntity<MessageResponse> logout(@Valid @RequestBody LogoutRequest logoutRequest) {
//        refreshTokenService.deleteByToken(logoutRequest.getRefreshToken());
//        return ResponseEntity.ok(new MessageResponse("User logged out successfully"));
//    }
}