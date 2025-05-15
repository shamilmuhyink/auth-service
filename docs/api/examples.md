# Auth Service API Usage Examples

This document provides practical examples of how to use the Auth Service API.

## Authentication Flow Examples

### Local Authentication Flow

#### 1. Register a new user

```bash
curl -X POST http://localhost:8080/api/auth/register \
  -H "Content-Type: application/json" \
  -d '{
    "username": "newuser",
    "password": "Password123!",
    "email": "newuser@example.com",
    "name": "New User"
  }'
```

#### 2. Login with the registered user

```bash
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "username": "newuser",
    "password": "Password123!"
  }'
```

Response:
```json
{
  "accessToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "tokenType": "Bearer",
  "expiresIn": 3600
}
```

#### 3. Access protected resources

```bash
curl -X GET http://localhost:8080/api/users/me \
  -H "Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..."
```

### OAuth2 Authentication Flow

#### 1. Initiate OAuth2 login with Google

Direct the user to:
```
http://localhost:8080/oauth2/authorize/google
```

The user will be redirected to Google's login page, and after successful authentication, they will be redirected back to your application with an authorization code.

#### 2. Handle the OAuth2 callback

Your application should have a frontend that handles the redirect and extracts the JWT token from the response.

## User Management Examples

### Update user profile

```bash
curl -X PUT http://localhost:8080/api/users/me \
  -H "Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..." \
  -H "Content-Type: application/json" \
  -d '{
    "name": "Updated Name",
    "email": "updated.email@example.com"
  }'
```

## Token Management Examples

### Refresh an expired token

```bash
curl -X POST http://localhost:8080/api/auth/refresh \
  -H "Content-Type: application/json" \
  -d '{
    "refreshToken": "your-refresh-token"
  }'
```

### Logout

```bash
curl -X POST http://localhost:8080/api/auth/logout \
  -H "Content-Type: application/json" \
  -d '{
    "refreshToken": "your-refresh-token"
  }'
```

## Integration Examples

### Frontend JavaScript Example

```javascript
// Login function
async function login(username, password) {
  try {
    const response = await fetch('http://localhost:8080/api/auth/login', {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
      },
      body: JSON.stringify({ username, password }),
    });
    
    if (!response.ok) {
      throw new Error('Login failed');
    }
    
    const data = await response.json();
    
    // Store the token in localStorage
    localStorage.setItem('accessToken', data.accessToken);
    localStorage.setItem('refreshToken', data.refreshToken);
    
    return data;
  } catch (error) {
    console.error('Login error:', error);
    throw error;
  }
}

// Get user profile
async function getUserProfile() {
  try {
    const token = localStorage.getItem('accessToken');
    
    const response = await fetch('http://localhost:8080/api/users/me', {
      method: 'GET',
      headers: {
        'Authorization': `Bearer ${token}`,
      },
    });
    
    if (!response.ok) {
      throw new Error('Failed to fetch user profile');
    }
    
    return await response.json();
  } catch (error) {
    console.error('Profile fetch error:', error);
    throw error;
  }
}
```

### OAuth2 Redirect Handling (React Example)

```javascript
import React, { useEffect } from 'react';
import { useLocation, useNavigate } from 'react-router-dom';

const OAuth2RedirectHandler = () => {
  const location = useLocation();
  const navigate = useNavigate();

  useEffect(() => {
    // Get the token from the URL
    const params = new URLSearchParams(location.search);
    const token = params.get('token');
    
    if (token) {
      // Store the token
      localStorage.setItem('accessToken', token);
      
      // Redirect to the home page
      navigate('/');
    } else {
      // Handle error
      navigate('/login?error=auth_failure');
    }
  }, [location, navigate]);

  return (
    <div>
      Processing authentication...
    </div>
  );
};

export default OAuth2RedirectHandler;
```