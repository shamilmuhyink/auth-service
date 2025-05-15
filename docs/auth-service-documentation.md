# Auth Service Documentation

## Overview
The Auth Service is a Spring Boot application that provides OAuth2 authentication capabilities. It supports both local user authentication and third-party OAuth2 providers like Google and GitHub.

## Project Structure

### Core Components

#### Main Application
- `AuthServiceApplication`: The entry point for the Spring Boot application.

#### Entity Models
- `User`: Represents a user in the system with fields for authentication and profile information.
- `Role`: Defines user roles for authorization purposes.

### Configuration

The application is configured using YAML-based configuration in `application.yml`:

- **Database Configuration**: PostgreSQL database connection settings
- **JPA Configuration**: Hibernate settings for ORM
- **OAuth2 Configuration**: Settings for Google and GitHub OAuth2 providers

## Authentication Flow

The service supports two main authentication flows:

1. **Local Authentication**: Username/password-based authentication
2. **OAuth2 Authentication**: Authentication via third-party providers (Google, GitHub)

### OAuth2 Provider Integration

The service is configured to work with:

- **Google**: Provides authentication using Google accounts
- **GitHub**: Provides authentication using GitHub accounts

## Database Schema

### Users Table
Stores user information including:
- User credentials (username, password)
- Personal information (email, name)
- OAuth2 provider details (if applicable)
- Account status

### Roles Table
Stores role definitions:
- ROLE_USER: Standard user permissions
- ROLE_ADMIN: Administrative permissions

### User_Roles Table
Junction table that maps users to their assigned roles.

## Security Implementation

The application uses Spring Security with:
- JWT-based authentication
- OAuth2 client and resource server capabilities
- Role-based authorization

## Development Setup

### Prerequisites
- Java 17
- PostgreSQL database
- OAuth2 client credentials for Google and GitHub

### Environment Variables
The following environment variables need to be set:
- `OAUTH_GOOGLE_CLIENT_ID`: Google OAuth client ID
- `OAUTH_GOOGLE_CLIENT_SECRET`: Google OAuth client secret
- `OAUTH_GITHUB_CLIENT_ID`: GitHub OAuth client ID
- `OAUTH_GITHUB_CLIENT_SECRET`: GitHub OAuth client secret

## API Endpoints

The service likely exposes endpoints for:
- User registration
- Authentication (login)
- OAuth2 callback handling
- User profile management
- Authorization verification

## Dependencies

The project uses the following key dependencies:
- Spring Boot Web
- Spring Data JPA
- Spring Security
- Spring OAuth2 Client
- Spring OAuth2 Resource Server
- PostgreSQL Driver
- JJWT for JWT handling
- Lombok for reducing boilerplate code