# Development Environment Setup

This guide provides instructions for setting up a development environment for our service.

## Prerequisites

- Java Development Kit (JDK) 17 or higher
- Maven 3.6+ or Gradle 7.0+
- PostgreSQL 13+
- Git
- IDE (IntelliJ IDEA, Eclipse, or VS Code with Java extensions)
- Postman or similar tool for API testing

## Local Setup Steps

### 1. Clone the Repository

```bash
git clone https://github.com/your-organization/service-name.git
cd service-name
```

### 2. Configure PostgreSQL

1. Install PostgreSQL if not already installed
2. Create a new database for the application:

```sql
CREATE DATABASE service_db;
CREATE USER service_user WITH ENCRYPTED PASSWORD 'your_password';
GRANT ALL PRIVILEGES ON DATABASE service_db TO service_user;
```

### 3. Configure OAuth2 Providers

#### Google OAuth2 Setup

1. Go to the [Google Developer Console](https://console.developers.google.com/)
2. Create a new project
3. Enable the Google+ API
4. Create OAuth2 credentials
   - Application type: Web application
   - Authorized redirect URIs: `http://localhost:8080/oauth2/callback/google`
5. Note your Client ID and Client Secret

#### GitHub OAuth2 Setup

1. Go to [GitHub Developer Settings](https://github.com/settings/developers)
2. Create a new OAuth App
3. Set the Authorization callback URL to: `http://localhost:8080/oauth2/callback/github`
4. Note your Client ID and Client Secret

### 4. Configure Application Properties

Create a `application-dev.yml` file in the `src/main/resources` directory:

```yaml
spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/service_db
    username: service_user
    password: your_password
    driver-class-name: org.postgresql.Driver
  jpa:
    hibernate:
      ddl-auto: update
    show-sql: true
    properties:
      hibernate:
        format_sql: true
        dialect: org.hibernate.dialect.PostgreSQLDialect
  security:
    oauth2:
      client:
        registration:
          google:
            client-id: ${OAUTH_GOOGLE_CLIENT_ID}
            client-secret: ${OAUTH_GOOGLE_CLIENT_SECRET}
            redirect-uri: "{baseUrl}/oauth2/callback/{registrationId}"
            scope:
              - email
              - profile
          github:
            client-id: ${OAUTH_GITHUB_CLIENT_ID}
            client-secret: ${OAUTH_GITHUB_CLIENT_SECRET}
            redirect-uri: "{baseUrl}/oauth2/callback/{registrationId}"
            scope:
              - user:email
              - read:user

# JWT Configuration
app:
  auth:
    tokenSecret: your-jwt-secret-key-here-should-be-at-least-32-characters
    tokenExpirationMsec: 3600000  # 1 hour
    refreshTokenExpirationMsec: 2592000000  # 30 days

# Server Configuration
server:
  port: 8080
```

### 5. Set Environment Variables

Set the following environment variables:

```bash
# For Linux/macOS
export OAUTH_GOOGLE_CLIENT_ID=your-google-client-id
export OAUTH_GOOGLE_CLIENT_SECRET=your-google-client-secret
export OAUTH_GITHUB_CLIENT_ID=your-github-client-id
export OAUTH_GITHUB_CLIENT_SECRET=your-github-client-secret

# For Windows
set OAUTH_GOOGLE_CLIENT_ID=your-google-client-id
set OAUTH_GOOGLE_CLIENT_SECRET=your-google-client-secret
set OAUTH_GITHUB_CLIENT_ID=your-github-client-id
set OAUTH_GITHUB_CLIENT_SECRET=your-github-client-secret
```

### 6. Build the Application

Using Maven:
```bash
mvn clean install
```

Using Gradle:
```bash
./gradlew clean build
```

### 7. Run the Application

Using Maven:
```bash
mvn spring-boot:run -Dspring-boot.run.profiles=dev
```

Using Gradle:
```bash
./gradlew bootRun --args='--spring.profiles.active=dev'
```

The application should now be running at `http://localhost:8080`.

## IDE Setup

### IntelliJ IDEA

1. Import the project as a Maven/Gradle project
2. Configure Run Configuration:
   - Main class: `com.yourcompany.service.ServiceApplication`
   - VM options: `-Dspring.profiles.active=dev`
   - Environment variables: Set the OAuth2 client IDs and secrets

### Eclipse

1. Import as Maven/Gradle project
2. Configure Run Configuration:
   - Main class: `com.yourcompany.service.ServiceApplication`
   - VM arguments: `-Dspring.profiles.active=dev`
   - Environment: Set the OAuth2 client IDs and secrets

### VS Code

1. Install Java Extension Pack
2. Open the project folder
3. Configure `.vscode/launch.json`:
```json
{
  "configurations": [
    {
      "type": "java",
      "name": "Spring Boot-ServiceApplication",
      "request": "launch",
      "mainClass": "com.yourcompany.service.ServiceApplication",
      "projectName": "service-name",
      "args": "--spring.profiles.active=dev",
      "env": {
        "OAUTH_GOOGLE_CLIENT_ID": "your-google-client-id",
        "OAUTH_GOOGLE_CLIENT_SECRET": "your-google-client-secret",
        "OAUTH_GITHUB_CLIENT_ID": "your-github-client-id",
        "OAUTH_GITHUB_CLIENT_SECRET": "your-github-client-secret"
      }
    }
  ]
}
```

## Testing the Setup

1. Start the application
2. Open a browser and navigate to `http://localhost:8080/api/status` (assuming you have a status endpoint)
3. You should see a response indicating the service is running

## Troubleshooting

### Database Connection Issues
- Verify PostgreSQL is running: `pg_isready`
- Check database credentials in application-dev.yml
- Ensure the database exists: `psql -l`

### OAuth2 Configuration Issues
- Verify redirect URIs in provider settings match your application configuration
- Check environment variables are correctly set
- Examine application logs for OAuth2 related errors

### JWT Token Issues
- Ensure the token secret is sufficiently long and secure
- Check token expiration settings
