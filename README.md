# Auth Service

## Overview
A secure authentication and authorization service built with Spring Boot that provides JWT-based authentication and OAuth2 integration with providers such as Google and GitHub.

## Features
- Local username/password authentication
- OAuth2 authentication (Google, GitHub)
- JWT token-based authorization with refresh token rotation
- Role-based access control
- Secure password storage
- Account management

## Tech Stack
- Java 17
- Spring Boot
- Spring Security with JWT
- PostgreSQL
- Docker
- Lombok

## Getting Started

### Prerequisites
- Java 17
- PostgreSQL
- Maven
- Docker (optional, for containerized deployment)

### Configuration
Configure the application in `src/main/resources/application.yml`:
- Database settings
- JWT configuration
- OAuth2 provider credentials
- CORS settings

### Environment Variables
```
OAUTH_GOOGLE_CLIENT_ID=your-google-client-id OAUTH_GOOGLE_CLIENT_SECRET=your-google-client-secret OAUTH_GITHUB_CLIENT_ID=your-github-client-id OAUTH_GITHUB_CLIENT_SECRET=your-github-client-secret SPRING_DATASOURCE_URL=jdbc:postgresql://localhost:5432/auth_db SPRING_DATASOURCE_USERNAME=postgres SPRING_DATASOURCE_PASSWORD=postgres JWT_SECRET=your-secure-jwt-secret
```

### Running Locally
```
bash mvn spring-boot:run
```

### Building and Running with Docker
```
bash docker-compose up -d
```

## API Documentation
API documentation is available at `/swagger-ui.html` when the application is running.

## Contributing
See the [contributing guide](docs/development/CONTRIBUTING.md) for detailed information.

## License
[Specify your license]

