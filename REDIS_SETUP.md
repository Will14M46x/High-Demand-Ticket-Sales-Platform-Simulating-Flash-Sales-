# Redis Setup with Docker

Redis is now configured to run via Docker for rate limiting in the Auth Service.

## 🚀 Quick Start

### 1. Start Docker Desktop
Make sure Docker Desktop is installed and running on your machine.
- Download from: https://www.docker.com/products/docker-desktop

### 2. Start Redis
You have two options:

#### Option A: Using PowerShell Script (Recommended)
```powershell
.\start-redis.ps1
```

#### Option B: Using Docker Compose Directly
```powershell
docker-compose up -d redis
```

or (if you have newer Docker)

```powershell
docker compose up -d redis
```

### 3. Verify Redis is Running
```powershell
docker ps --filter "name=ticket-redis"
```

You should see the `ticket-redis` container running.

## 📊 Redis Configuration

- **Container Name**: `ticket-redis`
- **Image**: `redis:7.0-alpine`
- **Port**: `6379` (mapped to localhost:6379)
- **Purpose**: Rate limiting for Auth Service

## ✅ What This Enables

Once Redis is running, the Auth Service will:
- ✅ Track failed login attempts
- ✅ Lock accounts after 5 failed attempts
- ✅ Unlock accounts after 15 minutes
- ✅ Store rate limiting data in Redis

## 🔧 Useful Commands

### Start Redis
```powershell
docker start ticket-redis
```

### Stop Redis
```powershell
docker stop ticket-redis
```

### View Redis Logs
```powershell
docker logs ticket-redis
```

### View Redis Logs (follow mode)
```powershell
docker logs -f ticket-redis
```

### Remove Redis Container
```powershell
docker stop ticket-redis
docker rm ticket-redis
```

### Restart Redis
```powershell
docker restart ticket-redis
```

### Connect to Redis CLI (for debugging)
```powershell
docker exec -it ticket-redis redis-cli
```

Inside Redis CLI, you can run:
- `KEYS *` - List all keys
- `GET lockout:email@example.com` - Check if an email is locked out
- `FLUSHALL` - Clear all data (use with caution!)

## 🔄 Restart Auth Service

After starting Redis, **restart the Auth Service** for rate limiting to take effect:

1. Stop the current Auth Service (if running)
2. Start it again - it will now connect to Redis

## ⚠️ Troubleshooting

### Docker Desktop Not Running
**Error**: `error during connect: open //./pipe/dockerDesktopLinuxEngine`

**Solution**: Start Docker Desktop application

### Port 6379 Already in Use
**Error**: `Bind for 0.0.0.0:6379 failed: port is already allocated`

**Solution**: 
1. Check if Redis is already running: `docker ps`
2. If another Redis instance is running, stop it or change the port in `docker-compose.yml`

### Auth Service Can't Connect to Redis
**Error**: `Unable to connect to Redis`

**Solutions**:
1. Verify Redis is running: `docker ps --filter "name=ticket-redis"`
2. Check Redis logs: `docker logs ticket-redis`
3. Verify port 6379 is accessible: `Test-NetConnection localhost -Port 6379` (PowerShell)

## 📝 Configuration Files

- **docker-compose.yml**: Redis service configuration
- **auth-service/src/main/resources/application.properties**: Redis connection settings
  - `spring.data.redis.host=localhost`
  - `spring.data.redis.port=6379`
  - `rate-limit.enabled=true`

## 💡 Rate Limiting Behavior

- **Max Failed Attempts**: 5
- **Lockout Duration**: 15 minutes
- **Attempt Window**: 30 minutes (failed attempts reset after 30 min)
- **Tracking**: Login attempts stored in both:
  - Redis (for fast rate limiting checks)
  - Database (for audit trail)

## 🎯 Next Steps

1. ✅ Start Redis: `.\start-redis.ps1`
2. ✅ Restart Auth Service
3. ✅ Test signup/login - rate limiting is now active!

