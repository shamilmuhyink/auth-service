# Auth Service Architecture Diagram

The following diagram illustrates the high-level architecture of the Auth Service.

```mermaid
graph TD
    Client[Client Application] -->|Authentication Request| AuthController
    
    subgraph "Auth Service"
        AuthController[Auth Controller] -->|Process Auth| AuthService
        AuthController -->|OAuth2 Login| OAuth2AuthHandler
        
        AuthService[Auth Service] -->|Validate User| UserRepository
        AuthService -->|Generate Token| JwtTokenProvider
        
        OAuth2AuthHandler[OAuth2 Auth Handler] -->|Process OAuth2| OAuth2UserService
        OAuth2UserService[OAuth2 User Service] -->|Create/Update User| UserRepository
        OAuth2UserService -->|Generate Token| JwtTokenProvider
        
        JwtTokenProvider[JWT Token Provider] -->|Store Token| TokenRepository
        
        UserController[User Controller] -->|User Operations| UserService
        UserService[User Service] -->|CRUD Operations| UserRepository
        
        UserRepository[(User Repository)]
        TokenRepository[(Token Repository)]
    end
    
    subgraph "External OAuth2 Providers"
        Google[Google]
        GitHub[GitHub]
    end
    
    OAuth2AuthHandler -->|Authenticate| Google
    OAuth2AuthHandler -->|Authenticate| GitHub
    Google -->|User Info| OAuth2AuthHandler
    GitHub -->|User Info| OAuth2AuthHandler
    
    Database[(PostgreSQL Database)]
    UserRepository -->|Persist Data| Database
    TokenRepository -->|Persist Data| Database
```

## Component Descriptions

### Client-Facing Components
- **Auth Controller**: Handles authentication requests, including login, registration, and token refresh
- **User Controller**: Manages user profile operations

### Core Services
- **Auth Service**: Implements authentication logic and user validation
- **OAuth2 Auth Handler**: Manages OAuth2 authentication flow with external providers
- **OAuth2 User Service**: Processes user information from OAuth2 providers
- **User Service**: Handles user-related operations
- **JWT Token Provider**: Generates and validates JWT tokens

### Data Access
- **User Repository**: Manages user entity persistence
- **Token Repository**: Manages token storage and validation

### External Integrations
- **OAuth2 Providers**: External authentication providers (Google, GitHub)

### Data Storage
- **PostgreSQL Database**: Persistent storage for user and token information