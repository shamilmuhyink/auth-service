# Auth Service API Documentation

## Authentication Endpoints

### Register User
- **POST** `/api/auth/register`
- **Description**: Register a new user with the service
- **Request Body**:
  ```json
  {
    "username": "user123",
    "password": "securePassword123",
    "email": "user@example.com",
    "name": "John Doe"
  }
  ```
- **Response**: `201 Created`
  ```json
  {
    "id": "1",
    "username": "user123",
    "email": "user@example.com",
    "name": "John Doe",
    "createdAt": "2023-06-15T10:30:45Z"
  }
  ```

### Login
- **POST** `/api/auth/login`
- **Description**: Authenticate a user and receive tokens
- **Request Body**:
  ```json
  {
    "username": "user123",
    "password": "securePassword123"
  }
  ```
- **Response**: `200 OK`
  ```json
  {
    "accessToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
    "refreshToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
    "tokenType": "Bearer",
    "expiresIn": 3600
  }
  ```

### Refresh Token
- **POST** `/api/auth/refresh`
- **Description**: Obtain a new access token using a refresh token
- **Request Body**:
  ```json
  {
    "refreshToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..."
  }
  ```
- **Response**: `200 OK`
  ```json
  {
    "accessToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
    "refreshToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
    "tokenType": "Bearer",
    "expiresIn": 3600
  }
  ```

### Logout
- **POST** `/api/auth/logout`
- **Description**: Invalidate a refresh token
- **Request Body**:
  ```json
  {
    "refreshToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..."
  }
  ```
- **Response**: `200 OK`
  ```json
  {
    "message": "Logged out successfully"
  }
  ```

## OAuth2 Endpoints

### OAuth2 Login
- **GET** `/oauth2/authorize/{provider}`
- **Description**: Redirect to OAuth2 provider login page
- **Path Parameters**:
  - `provider`: OAuth2 provider (google, github)
- **Response**: `302 Found` (Redirect to provider)

### OAuth2 Callback
- **GET** `/oauth2/callback/{provider}`
- **Description**: Handle OAuth2 provider callback
- **Path Parameters**:
  - `provider`: OAuth2 provider (google, github)
- **Response**: `302 Found` (Redirect to frontend with tokens)

## User Management Endpoints

### Get Current User
- **GET** `/api/users/me`
- **Description**: Get the profile of the currently authenticated user
- **Headers**:
  - `Authorization: Bearer {accessToken}`
- **Response**: `200 OK`
  ```json
  {
    "id": "1",
    "username": "user123",
    "email": "user@example.com",
    "name": "John Doe",
    "roles": ["ROLE_USER"],
    "createdAt": "2023-06-15T10:30:45Z",
    "lastLoginAt": "2023-06-16T08:45:12Z"
  }
  ```

### Update User
- **PUT** `/api/users/me`
- **Description**: Update the current user's profile
- **Headers**:
  - `Authorization: Bearer {accessToken}`
- **Request Body**:
  ```json
  {
    "name": "John Smith",
    "email": "john.smith@example.com"
  }
  ```
- **Response**: `200 OK`
  ```json
  {
    "id": "1",
    "username": "user123",
    "email": "john.smith@example.com",
    "name": "John Smith",
    "roles": ["ROLE_USER"],
    "createdAt": "2023-06-15T10:30:45Z",
    "updatedAt": "2023-06-17T14:20:33Z"
  }
  ```

### Change Password
- **POST** `/api/users/me/change-password`
- **Description**: Change the current user's password
- **Headers**:
  - `Authorization: Bearer {accessToken}`
- **Request Body**:
  ```json
  {
    "currentPassword": "securePassword123",
    "newPassword": "evenMoreSecure456"
  }
  ```
- **Response**: `200 OK`
  ```json
  {
    "message": "Password updated successfully"
  }
  ```