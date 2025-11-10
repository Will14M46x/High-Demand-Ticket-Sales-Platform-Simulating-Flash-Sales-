# High-Demand Ticket Sales Platform (Simulating Flash Sales)

A production-ready microservices-based ticket booking system designed to handle extreme concurrency and ensure absolute inventory integrity under peak load.

## Project Overview

This platform simulates flash sales scenarios where thousands of users attempt to purchase limited inventory simultaneously. The system is engineered to prevent overselling while maintaining high performance and reliability.

## Services

### Completed Services
- **Auth Service** - User authentication with Firebase & JWT (Port 8081)
- **Inventory Service** - Event and ticket inventory management (Port 8082)
- **Waiting Room Service** - Queue management for load control (Port 8083)

### In Development
- **Booking Service** - Ticket reservation and order management (Port 8084)
- **Payment Service** - Payment processing integration (Port 8085)

## Architecture

```
┌─────────────────────────────────────────────────────────────┐
│                        API Gateway                           │
│                   (Future Enhancement)                       │
└──────────────────────┬──────────────────────────────────────┘
                       │
        ┌──────────────┼──────────────┬───────────────┐
        │              │               │               │
┌───────▼─────┐ ┌─────▼──────┐ ┌─────▼──────┐ ┌─────▼──────┐
│    Auth     │ │ Inventory  │ │  Booking   │ │  Payment   │
│  Service    │ │  Service   │ │  Service   │ │  Service   │
│  (8081)     │ │  (8082)    │ │  (8084)    │ │  (8085)    │
└──────┬──────┘ └──────┬─────┘ └──────┬─────┘ └──────┬─────┘
       │               │               │               │
       ▼               ▼               ▼               ▼
   ┌────────────────────────────────────────────────────────┐
   │                   MySQL Database                        │
   └────────────────────────────────────────────────────────┘
                              │
                              ▼
                   ┌──────────────────┐
                   │  Redis Cache     │
                   │  (Queue/Holds)   │
                   └──────────────────┘
```

## Quick Start

### Prerequisites

- **Java 17+**
- **Maven 3.6+**
- **MySQL 8.0+**
- **Redis** (for Waiting Room and Booking services)
- **(Optional) Firebase Account** for production auth

### 1. Clone Repository

```bash
git clone https://github.com/Will14M46x/High-Demand-Ticket-Sales-Platform-Simulating-Flash-Sales-.git
cd High-Demand-Ticket-Sales-Platform-Simulating-Flash-Sales-
```

### 2. Setup Databases

```sql
CREATE DATABASE ticket_auth_db;
CREATE DATABASE ticket_inventory_db;
CREATE DATABASE ticket_booking_db;
CREATE DATABASE ticket_payment_db;
```

### 3. Build All Services

```bash
# Build parent POM first
mvn clean install -N

# Build all services
mvn clean install
```

### 4. Start Services

Start each service in a separate terminal:

```bash
# Auth Service
cd auth-service
mvn spring-boot:run

# Inventory Service
cd inventory-service/inventory_service
./gradlew bootRun

# Waiting Room Service
cd waiting-room-service
mvn spring-boot:run

# (Add others as they're developed)
```

### 5. Verify Services

```bash
# Auth Service
curl http://localhost:8081/api/auth/health

# Inventory Service
curl http://localhost:8082/api/inventory/health

# Waiting Room Service
curl http://localhost:8083/api/waiting-room/health
```

## Service Documentation

| Service | Port | Documentation | Status |
|---------|------|--------------|--------|
| Auth Service | 8081 | [README](auth-service/README.md) | ✅ Complete |
| Inventory Service | 8082 | [README](inventory-service/Inventory%20README) | ✅ Complete |
| Waiting Room Service | 8083 | - | ✅ Complete |
| Booking Service | 8084 | - | 🚧 In Progress |
| Payment Service | 8085 | - | 🚧 Planned |

## Auth Service

The authentication service provides centralized user management and JWT-based authentication for all microservices.

### Features
- User registration and login
- Firebase Authentication integration
- JWT token generation and validation
- Secure password handling
- OAuth 2.0 support

### Quick Test

```bash
# Sign up a new user
curl -X POST http://localhost:8081/api/auth/signup \
  -H "Content-Type: application/json" \
  -d '{
    "email": "test@example.com",
    "password": "password123",
    "name": "Test User"
  }'

# Response includes JWT token
{
  "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "tokenType": "Bearer",
  "userId": 1,
  "email": "test@example.com",
  "name": "Test User",
  "firebaseUid": "mock-uid-abc123"
}
```

### Integration

Other services can validate JWT tokens:

```bash
curl -X GET http://localhost:8081/api/auth/validate-token \
  -H "Authorization: Bearer YOUR_JWT_TOKEN"
```

See the Auth Service README for integration instructions.

## Technology Stack

### Backend
- **Framework**: Spring Boot 3.3.0
- **Language**: Java 17
- **Build Tools**: Maven & Gradle
- **Architecture**: Microservices

### Databases
- **MySQL**: Persistent storage for users, events, orders
- **Redis**: Volatile data, queues, rate limiting, ticket holds

### Security & Authentication
- **Spring Security**: Authorization and authentication
- **Firebase Admin SDK**: User authentication
- **JWT**: Stateless authentication tokens
- **OAuth 2.0**: Third-party authentication

### Additional Technologies
- **Lombok**: Reduce boilerplate code
- **JPA/Hibernate**: ORM for database operations
- **Validation**: Bean validation
- **REST**: Inter-service communication

## Project Structure

```
├── auth-service/              # Authentication service
│   ├── src/
│   ├── pom.xml
│   ├── README.md
│   └── POSTMAN_COLLECTION.json
├── inventory-service/         # Inventory management
│   ├── inventory_service/
│   └── pom.xml
├── booking-service/           # Booking and order management
│   └── pom.xml
├── payment-service/           # Payment processing
│   └── pom.xml
├── waiting-room-service/      # Queue management
│   └── pom.xml
├── common/                    # Shared utilities
│   └── pom.xml
├── pom.xml                    # Parent POM
└── mvnw.cmd                   # Maven wrapper
```

## Core Requirements

### 1. Inventory Integrity (CRITICAL)
- ✅ Atomic inventory updates with locking mechanisms
- ✅ Never oversell tickets
- ✅ Real-time inventory tracking
- 🚧 Temporary hold mechanism (5-10 min) using Redis

### 2. Concurrency Control
- ✅ Waiting Room queue management
- 🚧 Distributed locking for ticket reservation
- 🚧 Rate limiting per user
- 🚧 Load shedding under extreme load

### 3. Authentication & Security
- ✅ OAuth 2.0 integration (Firebase)
- ✅ JWT token-based authentication
- ✅ Secure password handling
- ✅ Session management

### 4. Third-Party Integration
- ✅ Firebase Authentication
- 🚧 Payment Gateway (Mock/Sandbox)

### 5. Fault Tolerance
- 🚧 Circuit breakers
- 🚧 Retry mechanisms
- 🚧 Graceful degradation
- 🚧 Health checks and monitoring

## Testing

### Load Testing
- **Tool**: JMeter
- **Scenario**: Simultaneous users purchasing limited inventory
- **Success Criteria**: No overselling, acceptable response times
- **Report Due**: Week 11

### Unit Testing
```bash
cd auth-service
mvn test
```

### Integration Testing
```bash
mvn verify
```

### API Testing
- Import Postman collections from each service
- Test authentication flow
- Test booking flow
- Test payment flow

## Project Timeline

| Week | Milestone | Status |
|------|-----------|--------|
| 7-10 | Core Development | ✅ In Progress |
| 10 | Proof of Concept (POC) | 🎯 Next Milestone |
| 11 | Load Testing & Demo | 📅 Upcoming |
| 12 | Final Documentation | 📅 Planned |

## Development Guidelines

### Code Quality
- Follow Spring Boot best practices
- Write meaningful commit messages
- Maintain code documentation
- Write unit tests for new features

### Git Workflow
- Create feature branches
- Require >= 2 peer reviews per PR
- Use meaningful branch names
- Keep commits atomic

### API Design
- RESTful conventions
- Proper HTTP status codes
- Consistent error responses
- Versioning strategy

## Known Issues & Limitations

1. Firebase is optional - can run in mock mode for local development
2. Parent POM must be built before services (`mvn clean install -N`)
3. Redis required for Waiting Room and Booking services
4. No API Gateway yet - direct service communication

## Deployment

### Local Development
- See individual service README files
- Use `application.properties` for configuration

### Production (Future)
- Docker containers
- Kubernetes orchestration
- Environment-based configuration
- CI/CD with GitHub Actions

## Configuration

### Auth Service
```properties
server.port=8081
spring.datasource.url=jdbc:mysql://localhost:3306/ticket_auth_db
firebase.enabled=false  # true for production
jwt.secret=YOUR_SECRET_KEY_HERE
```

### Inventory Service
```properties
server.port=8082
spring.datasource.url=jdbc:mysql://localhost:3306/ticket_inventory_db
```

### Waiting Room Service
```properties
server.port=8083
spring.redis.host=localhost
spring.redis.port=6379
```

## Contributing

This is a team project. Please:
1. Create a feature branch
2. Make your changes
3. Write/update tests
4. Submit PR with >= 2 reviewers
5. Update documentation

## Support & Contact

- **Project Lead**: [Team Lead Name]
- **Auth Service**: [Your Name]
- **Inventory Service**: [Team Member]
- **Booking Service**: [Team Member]

## License

This project is developed as part of an academic assignment.

## Academic Context

**Course**: Distributed Systems / Microservices Architecture  
**Institution**: University of Limerick  
**Semester**: [Current Semester]

## Useful Links

- [Spring Boot Documentation](https://spring.io/projects/spring-boot)
- [Firebase Admin SDK](https://firebase.google.com/docs/admin/setup)
- [JWT.io](https://jwt.io/)
- [Redis Documentation](https://redis.io/documentation)
- [JMeter Load Testing](https://jmeter.apache.org/)

---

**Last Updated**: November 10, 2024  
**Version**: 1.0.0  
**Status**: Active Development

