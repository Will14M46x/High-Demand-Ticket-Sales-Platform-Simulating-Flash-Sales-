# Authentication Service

Authentication microservice for the High-Demand Ticket Sales Platform. Handles user registration, login, and JWT-based authentication with Firebase integration.

## Features

- User registration with email and password validation
- User login with JWT token generation
- Firebase Authentication integration (OAuth 2.0)
- JWT token validation for protected endpoints
- Spring Security integration
- RESTful API design

## Technology Stack

- Java 17
- Spring Boot 3.3.0
- Spring Security
- Spring Data JPA
- MySQL / H2 Database
- Firebase Admin SDK
- JWT (jjwt 0.11.5)
- Maven

## Prerequisites

- Java 17 or higher
- Maven (or use included Maven Wrapper)
- MySQL database (or use H2 for testing)
- (Optional) Firebase project for production

## Build and Run

### Quick Start (Testing with H2)

```bash
# Build parent POM first (from project root)
mvnw clean install -N

# Build auth service
cd auth-service
../mvnw clean package

# Run with H2 database (no MySQL needed)
java -jar target/auth-service-1.0.0.jar --spring.profiles.active=test
```

### Production Setup (with MySQL)

1. Create database:
```sql
CREATE DATABASE ticket_auth_db;
```

2. Update `application.properties`:
```properties
spring.datasource.username=your_username
spring.datasource.password=your_password
```

3. Run:
```bash
mvn spring-boot:run
```

### Firebase Setup (Optional)

For production with Firebase:

1. **Get Firebase Service Account JSON:**
   - Go to [Firebase Console](https://console.firebase.google.com/) > Project Settings > Service Accounts
   - Click "Generate New Private Key"
   - Save as `firebase-service-account.json` in `src/main/resources/`

2. **Get Firebase Web API Key (CRITICAL for password verification):**
   - Go to [Firebase Console](https://console.firebase.google.com/) > Project Settings > General
   - Copy the "Web API Key" under "Your apps" section
   
3. **Update `application.properties`:**
```properties
firebase.enabled=true
firebase.config.path=classpath:firebase-service-account.json
firebase.api.key=YOUR_FIREBASE_WEB_API_KEY
```

⚠️ **SECURITY NOTE:** The `firebase.api.key` is **REQUIRED** for the `/api/auth/login` endpoint to verify passwords. Without this, the login endpoint would have a critical security vulnerability where anyone could login as any user with just their email.

**Recommended Authentication Flow:**
- **For Production:** Use `/api/auth/verify-firebase-token` (client authenticates with Firebase, sends token to backend)
- **For Development/Testing:** Use `/api/auth/login` with `firebase.enabled=false` (disables password verification - INSECURE)

Service runs on `http://localhost:8081`

## API Endpoints

### Public Endpoints (No Authentication Required)

#### 1. Health Check
```http
GET /api/auth/health
```

**Response:**
```json
{
  "status": "UP",
  "service": "auth-service"
}
```

#### 2. User Signup
```http
POST /api/auth/signup
Content-Type: application/json

{
  "email": "user@example.com",
  "password": "password123",
  "name": "John Doe",
  "phoneNumber": "+1234567890"
}
```

**Response (201 Created):**
```json
{
  "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "tokenType": "Bearer",
  "userId": 1,
  "email": "user@example.com",
  "name": "John Doe",
  "firebaseUid": "firebase-uid-123"
}
```

#### 3. User Login
```http
POST /api/auth/login
Content-Type: application/json

{
  "email": "user@example.com",
  "password": "password123"
}
```

**Response (200 OK):**
```json
{
  "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "tokenType": "Bearer",
  "userId": 1,
  "email": "user@example.com",
  "name": "John Doe",
  "firebaseUid": "firebase-uid-123"
}
```

#### 4. Verify Firebase Token (For Frontend Firebase Integration)
```http
POST /api/auth/verify-firebase-token
Content-Type: application/json

{
  "firebaseToken": "firebase-id-token-from-client"
}
```

**Response (200 OK):**
```json
{
  "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "tokenType": "Bearer",
  "userId": 1,
  "email": "user@example.com",
  "name": "John Doe",
  "firebaseUid": "firebase-uid-123"
}
```

#### 5. Validate JWT Token
```http
GET /api/auth/validate-token
Authorization: Bearer <jwt-token>
```

**Response (200 OK):**
```json
{
  "valid": true
}
```

### Protected Endpoints (Require JWT Token)

#### 6. Get User by ID
```http
GET /api/auth/user/{userId}
Authorization: Bearer <jwt-token>
```

**Response (200 OK):**
```json
{
  "id": 1,
  "email": "user@example.com",
  "name": "John Doe",
  "firebaseUid": "firebase-uid-123",
  "provider": "firebase",
  "phoneNumber": "+1234567890",
  "isActive": true,
  "isEmailVerified": false,
  "createdAt": "2024-11-10T10:00:00",
  "updatedAt": "2024-11-10T10:00:00"
}
```

## Integration with Other Services

Other microservices can validate tokens by:

1. **Calling validation endpoint:**
```http
GET /api/auth/validate-token
Authorization: Bearer <jwt-token>
```

2. **Implementing JWT validation locally** using the shared JWT secret (`jwt.secret` in properties)

## Error Responses

The service returns standard error responses:

**400 Bad Request** (Validation Error):
```json
{
  "status": 400,
  "errors": {
    "email": "Email should be valid",
    "password": "Password must be at least 6 characters"
  },
  "timestamp": "2024-11-10T10:00:00"
}
```

**401 Unauthorized** (Invalid Credentials):
```json
{
  "status": 401,
  "message": "Invalid credentials",
  "timestamp": "2024-11-10T10:00:00"
}
```

**404 Not Found** (User Not Found):
```json
{
  "status": 404,
  "message": "User not found with email: user@example.com",
  "timestamp": "2024-11-10T10:00:00"
}
```

**409 Conflict** (User Already Exists):
```json
{
  "status": 409,
  "message": "User with email user@example.com already exists",
  "timestamp": "2024-11-10T10:00:00"
}
```

## Testing

### Using Postman

Import `POSTMAN_COLLECTION.json` and set `baseUrl` to `http://localhost:8081`

### Using cURL

```bash
# Signup
curl -X POST http://localhost:8081/api/auth/signup \
  -H "Content-Type: application/json" \
  -d '{"email":"test@example.com","password":"password123","name":"Test User"}'

# Login
curl -X POST http://localhost:8081/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"test@example.com","password":"password123"}'
```

## Security

### Authentication & Authorization
- JWT-based stateless authentication
- Passwords handled by Firebase Authentication
- **Password verification via Firebase REST API** - ensures no user can login without valid credentials
- JWT tokens expire after 24 hours (configurable)
- Spring Security protection on all endpoints

### Password Verification
**Login Security:**
- The `/api/auth/login` endpoint verifies passwords using Firebase Authentication REST API
- Requires `firebase.api.key` to be configured when `firebase.enabled=true`
- Without proper configuration, the service will reject login attempts to prevent security vulnerabilities
- In development mode (`firebase.enabled=false`), password verification is bypassed (INSECURE - testing only)

### CORS Configuration
**Development:**
- Configured for localhost origins (ports 3000, 8080, 4200)
- Suitable for local testing

**Production:**
- ⚠️ **CRITICAL**: MUST specify exact allowed origins
- Never use wildcard (*) in production
- Set via environment variables: `CORS_ALLOWED_ORIGINS`
- Example: `https://yourdomain.com,https://www.yourdomain.com`

### Production Security Checklist
- [ ] Set unique JWT secret via `JWT_SECRET` environment variable
- [ ] **Set Firebase Web API Key via `FIREBASE_WEB_API_KEY` environment variable (CRITICAL)**
- [ ] Configure specific CORS origins (no wildcards)
- [ ] Enable HTTPS
- [ ] Set `cors.allow-credentials=true` only if needed with specific origins
- [ ] Use environment variables for all sensitive data
- [ ] Enable Firebase in production (`firebase.enabled=true`)
- [ ] Provide Firebase service account JSON file path
- [ ] Set secure cookie flags (`http-only`, `secure`)
- [ ] Review and update allowed methods and headers
- [ ] Prefer `/api/auth/verify-firebase-token` over `/api/auth/login` for production OAuth2 flow

## Configuration

Key settings in `application.properties`:

```properties
# Server
server.port=8081

# Database
spring.datasource.url=jdbc:mysql://localhost:3306/ticket_auth_db
spring.datasource.username=root
spring.datasource.password=root

# JWT
jwt.secret=<your-secret-key>
jwt.expiration=86400000

# Firebase
firebase.enabled=false

# CORS (Development)
cors.allowed-origins=http://localhost:3000,http://localhost:8080
cors.allowed-methods=GET,POST,PUT,DELETE,OPTIONS
cors.allowed-headers=*
cors.allow-credentials=false
cors.max-age=3600
```

### Environment-Specific Profiles

**Development** (`application.properties`):
- Permissive CORS for local development
- Multiple localhost ports allowed
- H2 database option

**Production** (`application-prod.properties`):
- **MUST** specify exact allowed origins (NO wildcards)
- Use environment variables for sensitive data
- Strict CORS configuration
- HTTPS-only cookies

**Example Production Environment Variables:**
```bash
export CORS_ALLOWED_ORIGINS=https://yourdomain.com,https://api.yourdomain.com
export JWT_SECRET=your-production-secret-key
export DB_PASSWORD=your-database-password
export FIREBASE_ENABLED=true
```

## Project Information

Part of the High-Demand Ticket Sales Platform - University of Limerick

For issues or questions, contact the development team.

