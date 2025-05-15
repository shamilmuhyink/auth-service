# Design Decisions and Rationale

This document outlines the key design decisions made for the Auth Service and the rationale behind them.

## Authentication Strategy

### Decision: Hybrid Authentication System (JWT + OAuth2)

We implemented a hybrid authentication system that supports both:
- Local username/password authentication with JWT tokens
- OAuth2-based authentication with multiple providers

**Rationale:**
- JWT provides stateless authentication, reducing database load for session management
- OAuth2 support simplifies user onboarding and reduces password management burden
- Hybrid approach gives users flexibility in how they authenticate
- Stateless JWT approach improves scalability in distributed environments

### Decision: Refresh Token Rotation

We implemented refresh token rotation, where each use of a refresh token invalidates it and issues a new one.

**Rationale:**
- Enhances security by limiting the lifetime of refresh tokens
- Reduces the risk of refresh token theft and reuse
- Provides a mechanism to revoke authentication when suspicious activity is detected

## OAuth2 Provider Selection

### Decision: Support for Google and GitHub

We chose to initially support Google and GitHub as OAuth2 providers.

**Rationale:**
- Google accounts are ubiquitous, covering a large user base
- GitHub integration is valuable for developer-focused applications
- Both providers have stable, well-documented OAuth2 implementations
- These providers offer good user profile data that can be mapped to our user model

## Database Schema Design

### Decision: Separate User and Authority Tables

We designed the database with separate tables for users and authorities/roles.

**Rationale:**
- Follows principle of separation of concerns
- Enables more flexible role management
- Supports future expansion to more complex permission systems
- Aligns with Spring Security's default expectations

### Decision: Store OAuth2 Provider Information

We store OAuth2 provider information and provider-specific user IDs in the user table.

**Rationale:**
- Enables linking multiple OAuth2 providers to a single user account
- Prevents duplicate accounts when users authenticate with different providers
- Facilitates account recovery and provider switching

## Security Measures

### Decision: Password Hashing with BCrypt

We use BCrypt for password hashing with a work factor of 12.

**Rationale:**
- BCrypt is designed specifically for password hashing
- Adaptive work factor allows increasing security as hardware improves
- Provides built-in salt generation and management
- Industry-standard approach with strong security properties

### Decision: Short-lived Access Tokens

We configured access tokens with a relatively short lifespan (1 hour) combined with longer-lived refresh tokens (30 days).

**Rationale:**
- Limits the window of opportunity if an access token is compromised
- Refresh token mechanism maintains good user experience despite short token lifetimes
- Balances security and convenience

## API Design

### Decision: RESTful API with JSON

We designed a RESTful API that uses JSON for data exchange.

**Rationale:**
- REST provides a standardized, resource-oriented approach
- JSON is lightweight and widely supported across platforms
- Familiar paradigm for frontend developers
- Good tooling and library support

### Decision: Versioned API Endpoints

We included API versioning in the URL path (e.g., `/api/v1/auth/login`).

**Rationale:**
- Enables evolution of the API without breaking existing clients
- Makes API changes more manageable
- Provides clear migration paths for client applications

## Technology Stack

### Decision: Spring Boot Framework

We chose Spring Boot as the primary framework.

**Rationale:**
- Comprehensive security features through Spring Security
- Excellent OAuth2 client and resource server support
- Robust data access through Spring Data
- Production-ready features like metrics, health checks, and externalized configuration
- Large community and extensive documentation

### Decision: PostgreSQL Database

We selected PostgreSQL as the database.

**Rationale:**
- Strong compliance with SQL standards
- Excellent support for JSON data types (useful for storing provider-specific data)
- Robust transaction support
- Good performance characteristics
- Open-source with strong community support

## Future Considerations

### Potential Enhancement: Multi-factor Authentication

We've designed the system to allow for future addition of multi-factor authentication.

**Rationale:**
- Increasing security requirements in many industries
- User expectations for additional security options
- Regulatory requirements in certain sectors

### Potential Enhancement: Additional OAuth2 Providers

The architecture supports easy addition of more OAuth2 providers.

**Rationale:**
- Different user bases may prefer different authentication providers
- Business requirements may change to target different user demographics
- Flexibility to adapt to changing market conditions