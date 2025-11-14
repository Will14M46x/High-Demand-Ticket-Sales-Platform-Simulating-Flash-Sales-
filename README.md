# High-Demand Ticket Sales Platform (Simulating Flash Sales)

A production-ready microservices-based ticket booking system designed to handle extreme concurrency and ensure absolute inventory integrity under peak load.

## Project Overview

This platform simulates flash sales scenarios where thousands of users attempt to purchase limited inventory simultaneously. The system is engineered to prevent overselling while maintaining high performance and reliability.

## Services

### ✅ All Services with Authentication Integrated!
- **Auth Service** - User authentication with Firebase & JWT (Port 8081) ✅
- **Inventory Service** - Event and ticket inventory management (Port 8082) ✅ **JWT Auth**
- **Waiting Room Service** - Queue management for load control (Port 8083) ✅ **JWT Auth**
- **Booking Service** - Ticket reservation and order management (Port 8084) ✅ **JWT Auth**

### In Development
- **Payment Service** - Payment processing integration (Port 8085)

### ✨ NEW: Frontend Application
- **React Frontend** - Modern web UI for testing all services (Port 3000) ✅ **COMPLETE**
  - User authentication & registration
  - Event browsing & creation
  - Waiting room simulation
  - Complete booking flow
  - Admin dashboard
  - 📖 See [Frontend Setup Guide](FRONTEND_SETUP.md)

## Architecture

```
┌─────────────────────────────────────────────────────────────┐
│                   React Frontend (3000)                      │
│         User Interface - Testing & Demonstration            │
└──────────────────────┬──────────────────────────────────────┘
                       │ HTTP/REST API Calls
                       │
┌──────────────────────┼──────────────────────────────────────┐
│                  API Gateway (Future)                        │
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

## 🚀 Quick Start

### ⚡ Authentication is Pre-Configured!

All services are already configured with Firebase Authentication and JWT tokens. **You only need 2 steps to get started:**

1. **Download Firebase Service Account JSON** (5 min)
2. **Create MySQL Database** (2 min)

### 📖 Complete Setup Guide

**👉 See [`SETUP_COMPLETE.md`](SETUP_COMPLETE.md) for full instructions! 👈**

Quick summary:
1. Download Firebase Service Account JSON from Firebase Console
2. Place it in `auth-service/src/main/resources/firebase/`
3. Create MySQL database: `CREATE DATABASE ticket_auth_db;`
4. Start services (all pre-configured!)

### Prerequisites

- **Java 17+**
- **Maven 3.6+** or **Gradle 8+**
- **MySQL 8.0+** running on localhost:3306
- **Redis** running on localhost:6379 (for Waiting Room and Booking services)
- **Firebase Account** - You already have: `andrew-ju-project`

### Start Services

```bash
# Terminal 1: Auth Service
cd auth-service && ../mvnw spring-boot:run

# Terminal 2: Inventory Service
cd inventory-service/inventory_service && ./gradlew bootRun

# Terminal 3: Booking Service
cd booking-service && ./gradlew bootRun

# Terminal 4: Waiting Room Service
cd waiting-room-service && ../mvnw spring-boot:run

# Terminal 5: Frontend (NEW!)
cd frontend && npm install && npm run dev
```

### Verify Services

```bash
curl http://localhost:8081/api/auth/health
curl http://localhost:8082/api/events/health
curl http://localhost:8083/waiting-room/health
curl http://localhost:8084/api/bookings/health
```

All should return `{"status":"UP"}`

### Access Frontend

Open browser to: **http://localhost:3000**

🎉 The frontend provides a complete UI for testing all services!

## Service Documentation

| Service | Port | Documentation | Status |
|---------|------|--------------|--------|
| **Frontend** | **3000** | **[Setup Guide](FRONTEND_SETUP.md)** · **[README](frontend/README.md)** | ✅ **Complete** |
| Auth Service | 8081 | [README](auth-service/README.md) | ✅ Complete |
| Inventory Service | 8082 | [README](inventory-service/Inventory%20README) | ✅ Complete |
| Waiting Room Service | 8083 | [`SETUP_COMPLETE.md`](SETUP_COMPLETE.md) | ✅ Complete |
| Booking Service | 8084 | [`SETUP_COMPLETE.md`](SETUP_COMPLETE.md) | ✅ Complete |
| Payment Service | 8085 | - | 🚧 Planned |

## 🔐 Authentication System

**✅ Production-ready authentication integrated across ALL services!**

### Features
- ✅ **Firebase Authentication** - OAuth 2.0 with Google's Firebase
- ✅ **JWT Tokens** - Stateless authentication across all microservices
- ✅ **Password Verification** - Secure password checking via Firebase
- ✅ **User Database** - MySQL storage for user data
- ✅ **Access Control** - Users can only access their own resources
- ✅ **Pre-configured** - Web API Key and JWT secrets already set!

### Your Firebase Project
- **Project**: andrew-ju-project
- **Web API Key**: AIzaSyAeKAKhBCoxnSDMjaC3qAAh5estwLkZvpk ✅
- **JWT Secret**: Generated and synchronized across all services ✅

### Quick Test (Option 1: Using Frontend - RECOMMENDED)

1. **Open browser**: http://localhost:3000
2. **Sign up**: Create a new account
3. **Create event**: Use the "Create Event" button
4. **Book tickets**: Click on event → Proceed to Booking → Complete purchase
5. **View bookings**: Check "My Bookings" page

**👉 See [FRONTEND_SETUP.md](FRONTEND_SETUP.md) for complete testing guide with UI!**

### Quick Test (Option 2: Using cURL)

```bash
# 1. Sign up a new user
curl -X POST http://localhost:8081/api/auth/signup \
  -H "Content-Type: application/json" \
  -d '{
    "email": "test@example.com",
    "password": "Test123456",
    "name": "Test User"
  }'

# 2. Use the returned token to create an event (protected operation)
curl -X POST http://localhost:8082/api/events \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer YOUR_TOKEN_HERE" \
  -d '{
    "name": "Concert",
    "location": "Arena",
    "date": "2024-12-31T20:00:00",
    "availableTickets": 100,
    "price": 50.0
  }'

# 3. Join waiting room
curl -X POST http://localhost:8083/waiting-room/join \
  -H "Authorization: Bearer YOUR_TOKEN_HERE"

# 4. Create booking
curl -X POST http://localhost:8084/api/bookings \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer YOUR_TOKEN_HERE" \
  -d '{"eventId":1,"quantity":2}'
```

**See [`SETUP_COMPLETE.md`](SETUP_COMPLETE.md) for backend API testing guide!**

## Technology Stack

### Frontend ✨ NEW
- **React 18**: Modern UI library
- **Vite**: Lightning-fast build tool
- **Tailwind CSS**: Utility-first styling
- **React Router**: Client-side routing
- **Zustand**: Lightweight state management
- **Axios**: HTTP client with interceptors
- **Lucide React**: Icon library
- **date-fns**: Date formatting

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
├── frontend/                  # ✨ NEW React Frontend Application
│   ├── src/
│   │   ├── components/       # Reusable UI components
│   │   ├── pages/            # Page components
│   │   ├── services/         # API client
│   │   ├── store/            # State management
│   │   └── config/           # Configuration
│   ├── package.json
│   ├── README.md             # Detailed frontend docs
│   ├── start.sh              # Quick start script
│   └── start.bat             # Windows start script
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
├── FRONTEND_SETUP.md          # ✨ Frontend setup guide
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

### 3. Authentication & Security ✅ COMPLETE
- ✅ OAuth 2.0 integration (Firebase)
- ✅ JWT token-based authentication across ALL services
- ✅ Secure password handling via Firebase
- ✅ Access control (users can only access their own data)
- ✅ Pre-configured with Web API Key and JWT secrets

### 4. Third-Party Integration
- ✅ Firebase Authentication (fully integrated)
- ✅ Firebase Admin SDK for user management
- ✅ Firebase REST API for password verification
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

