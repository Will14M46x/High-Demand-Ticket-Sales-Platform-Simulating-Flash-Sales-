# 🚀 Quick Start Guide - Run Platform in Browser

## Problem: Services Won't Start

If you see "JAVA_HOME is not set" or "JAVA_HOME is invalid" errors, follow these steps:

## Step 1: Find and Set JAVA_HOME

1. **Find your Java installation:**
   - Open PowerShell
   - Run: `where java`
   - This shows where Java is installed

2. **Find the JDK directory:**
   - Java might be in: `C:\Program Files\Java\jdk-21` (or jdk-17, jdk-19, etc.)
   - Or: `C:\Program Files\Eclipse Adoptium\jdk-21.x.x.x-hotspot`

3. **Set JAVA_HOME:**
   ```powershell
   $env:JAVA_HOME = "C:\Program Files\Java\jdk-21"
   # Replace with your actual JDK path
   ```

4. **Verify:**
   ```powershell
   Test-Path "$env:JAVA_HOME\bin\java.exe"
   # Should return: True
   ```

## Step 2: Set JWT_SECRET

```powershell
$env:JWT_SECRET = "your-super-secure-jwt-secret-here-minimum-256-bits-change-in-production"
```

## Step 3: Start Services One by One

**Terminal 1 - Auth Service (Port 8081):**
```powershell
cd auth-service
..\mvnw.cmd spring-boot:run
```
Wait until you see: `Started AuthServiceApplication`

**Terminal 2 - Inventory Service (Port 8082):**
```powershell
cd inventory-service
..\mvnw.cmd spring-boot:run
```

**Terminal 3 - Waiting Room (Port 8083):**
```powershell
cd waiting-room-service
..\mvnw.cmd spring-boot:run
```

**Terminal 4 - Booking Service (Port 8084):**
```powershell
cd booking-service
..\mvnw.cmd spring-boot:run
```

**Terminal 5 - Frontend (Port 3000):**
```powershell
cd frontend
npm install  # Only first time
npm run dev
```

## Step 4: Access in Browser

Once all services are running, open:
- **Frontend:** http://localhost:3000
- **Auth Service:** http://localhost:8081/api/auth/health
- **Inventory:** http://localhost:8082/api/events
- **Waiting Room:** http://localhost:8083/waiting-room/health
- **Booking:** http://localhost:8084/api/bookings/health

## Troubleshooting

### "JAVA_HOME is invalid"
- Find your JDK: `Get-ChildItem "C:\Program Files\Java" -Directory`
- Set: `$env:JAVA_HOME = "C:\Program Files\Java\jdk-21"` (replace with actual path)

### "Port already in use"
- Kill process on port: `netstat -ano | findstr :8081`
- Find PID and kill: `taskkill /PID <pid> /F`

### "Cannot connect to database"
- Services use H2 in-memory database (no setup needed)
- If using MySQL, make sure MySQL is running

### "Frontend won't start"
- Make sure Node.js is installed: `node --version`
- Install dependencies: `cd frontend && npm install`
- Try: `npm run dev`

## Expected Startup Time

- First run: 60-90 seconds per service (compiling)
- Subsequent runs: 15-30 seconds per service

