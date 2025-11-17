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

## Environment Configuration

### 🔒 Security: Never Commit Secrets!

All sensitive configuration values **MUST** be provided via environment variables. Never commit secrets to version control!

### Required Environment Variables

1. **Copy the example file:**
```bash
cp env.example .env
```

2. **Set required variables:**

**For Local Development/Testing:**
```bash
# JWT Configuration (REQUIRED)
export JWT_SECRET=$(openssl rand -base64 32)  # Generate a strong secret
export JWT_EXPIRATION=86400000

# Spring Profile
export SPRING_PROFILES_ACTIVE=test  # Use H2 database, no Firebase needed
```

**For Production:**
```bash
# JWT Configuration (REQUIRED - use strong secret!)
export JWT_SECRET=your-super-secure-jwt-secret-here-minimum-256-bits
export JWT_EXPIRATION=86400000

# Firebase Configuration (REQUIRED)
export FIREBASE_ENABLED=true
export FIREBASE_CONFIG_PATH=/path/to/firebase-service-account.json
export FIREBASE_WEB_API_KEY=your-firebase-web-api-key

# Database Configuration
export DB_URL=jdbc:mysql://localhost:3306/ticket_auth_db
export DB_USERNAME=root
export DB_PASSWORD=your-database-password

# CORS Configuration (use your actual domain!)
export CORS_ALLOWED_ORIGINS=https://yourdomain.com,https://www.yourdomain.com

# Spring Profile
export SPRING_PROFILES_ACTIVE=prod
```

⚠️ **WARNING:** The configuration files contain fallback values clearly marked as `INSECURE` or `TEST-ONLY`. These are for local development/testing **ONLY** and will log security warnings. Production deployments **MUST** set the `JWT_SECRET` environment variable.

### Generating Secure Secrets

```bash
# Generate a secure JWT secret (recommended)
openssl rand -base64 32

# Or use a UUID
uuidgen

# Generate a secure database password
openssl rand -base64 24
```

### Database Security Best Practices

**Create Dedicated Database User (Recommended for Production):**

```sql
-- Connect to MySQL as root
mysql -u root -p

-- Create dedicated user for auth service
CREATE USER 'auth_service_user'@'localhost' IDENTIFIED BY 'your-secure-password';

-- Grant only required privileges
GRANT SELECT, INSERT, UPDATE, DELETE ON ticket_auth_db.* TO 'auth_service_user'@'localhost';

-- For production, limit to specific host
CREATE USER 'auth_service_user'@'your-app-server-ip' IDENTIFIED BY 'your-secure-password';
GRANT SELECT, INSERT, UPDATE, DELETE ON ticket_auth_db.* TO 'auth_service_user'@'your-app-server-ip';

-- Flush privileges
FLUSH PRIVILEGES;
```

**Environment Configuration:**
```bash
export DB_USERNAME=auth_service_user
export DB_PASSWORD=your-secure-generated-password
```

⚠️ **NEVER use root credentials for application database access in production!**

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

**Secrets Management:**
- [ ] **Generate and set unique JWT secret via `JWT_SECRET` environment variable (CRITICAL)**
  - Use `openssl rand -base64 32` to generate
  - Minimum 256 bits (32+ characters)
  - NEVER commit to version control
- [ ] **Set Firebase Web API Key via `FIREBASE_WEB_API_KEY` environment variable (CRITICAL)**
- [ ] **Set database credentials via environment variables (CRITICAL)**
  - `DB_USERNAME` and `DB_PASSWORD`
  - NEVER use default credentials (root/root) in production
  - Create dedicated database user with minimal privileges
- [ ] Use a secure secret management system (AWS Secrets Manager, HashiCorp Vault, etc.)
- [ ] Rotate secrets regularly (recommended: JWT every 90 days, DB passwords every 180 days)

**Network & Access Control:**
- [ ] Configure specific CORS origins (no wildcards)
  - Set via `CORS_ALLOWED_ORIGINS` environment variable
  - Example: `https://yourdomain.com,https://www.yourdomain.com`
- [ ] Enable HTTPS/TLS for all connections
- [ ] Set `cors.allow-credentials=true` only if needed with specific origins
- [ ] Configure database to only accept connections from application server IPs

**Firebase Configuration:**
- [ ] Enable Firebase in production (`FIREBASE_ENABLED=true`)
- [ ] Provide Firebase service account JSON file path (`FIREBASE_CONFIG_PATH`)
- [ ] Ensure Firebase service account has minimal required permissions
- [ ] Prefer `/api/auth/verify-firebase-token` over `/api/auth/login` for production OAuth2 flow

**Application Security:**
- [ ] Set secure cookie flags (`http-only`, `secure`)
- [ ] Review and update allowed HTTP methods and headers
- [ ] Enable database connection pooling with reasonable limits
- [ ] Configure appropriate JPA hibernate.ddl-auto (use `validate` in production, not `update`)
- [ ] Set up database backups and disaster recovery procedures

**Code Quality:**
- [ ] All resources (FileInputStream, database connections) properly closed with try-with-resources
- [ ] No hardcoded credentials anywhere in codebase
- [ ] Security warnings reviewed and addressed at startup

## Configuration

### Environment Variable Usage

All sensitive configuration is externalized via environment variables. See `env.example` for a complete list.

**Configuration files use environment variables with fallbacks:**

```properties
# JWT Configuration
# Reads from JWT_SECRET env var (required)
jwt.secret=${JWT_SECRET}
jwt.expiration=${JWT_EXPIRATION:86400000}

# Firebase Configuration
firebase.enabled=${FIREBASE_ENABLED:false}
firebase.config.path=${FIREBASE_CONFIG_PATH:}
firebase.api.key=${FIREBASE_WEB_API_KEY:}

# Database Configuration (Production profile)
spring.datasource.url=${DB_URL:jdbc:mysql://localhost:3306/ticket_auth_db}
spring.datasource.username=${DB_USERNAME:root}
spring.datasource.password=${DB_PASSWORD}

# CORS Configuration
cors.allowed-origins=${CORS_ALLOWED_ORIGINS:http://localhost:3000,http://localhost:8080}
cors.allowed-methods=${CORS_ALLOWED_METHODS:GET,POST,PUT,DELETE,OPTIONS}
```

### Profile-Specific Configuration

**`test` profile** (`application-test.properties`):
- Uses H2 in-memory database
- Firebase disabled
- Test-only JWT secret with clear warning
- Permissive CORS for local testing

**`prod` profile** (`application-prod.properties`):
- **REQUIRES** environment variables for all secrets (no fallbacks)
- MySQL database
- Firebase enabled
- Strict CORS configuration

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

