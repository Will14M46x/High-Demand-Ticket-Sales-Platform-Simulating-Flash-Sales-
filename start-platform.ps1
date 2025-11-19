# Full Platform Launch Script - Fixed Version
# Launches all microservices and frontend with proper configuration

Write-Host "`n🎟️  High-Demand Ticket Sales Platform - Launching Full Stack" -ForegroundColor Cyan
Write-Host "=" * 70 -ForegroundColor Cyan
Write-Host ""

# Get script directory
$scriptDir = Split-Path -Parent $MyInvocation.MyCommand.Path
Set-Location $scriptDir

# Set JAVA_HOME
$env:JAVA_HOME = "C:\Program Files\Java\jdk-21"
$env:PATH = "$env:JAVA_HOME\bin;$env:PATH"

# Set JWT_SECRET if not set
if (-not $env:JWT_SECRET) {
    $env:JWT_SECRET = "your-super-secure-jwt-secret-here-minimum-256-bits-change-in-production"
    Write-Host "⚠️  JWT_SECRET not set, using default (INSECURE FOR PRODUCTION!)" -ForegroundColor Yellow
} else {
    Write-Host "✅ JWT_SECRET is set" -ForegroundColor Green
}

Write-Host "✅ JAVA_HOME set to: $env:JAVA_HOME" -ForegroundColor Green
Write-Host ""

# Check prerequisites
Write-Host "🔍 Checking Prerequisites..." -ForegroundColor Yellow

# Check Redis
$redisRunning = Test-NetConnection -ComputerName localhost -Port 6379 -InformationLevel Quiet -ErrorAction SilentlyContinue
if (-not $redisRunning) {
    Write-Host "⚠️  Redis is NOT running on port 6379" -ForegroundColor Yellow
    Write-Host "   Waiting Room and Booking services require Redis" -ForegroundColor Yellow
} else {
    Write-Host "✅ Redis is running" -ForegroundColor Green
}

# Check Java
try {
    $javaVersion = java -version 2>&1 | Select-Object -First 1
    Write-Host "✅ Java is available" -ForegroundColor Green
} catch {
    Write-Host "❌ Java is NOT found. Please install Java 17+" -ForegroundColor Red
    exit 1
}

Write-Host "`n🚀 Starting Services..." -ForegroundColor Cyan
Write-Host ""

# Start Auth Service (Port 8081)
Write-Host "[1/5] Starting Auth Service on port 8081..." -ForegroundColor Magenta
Start-Process powershell -ArgumentList "-NoExit", "-Command", "cd '$scriptDir\auth-service'; `$env:JAVA_HOME='$env:JAVA_HOME'; `$env:PATH='$env:PATH'; `$env:JWT_SECRET='$env:JWT_SECRET'; Write-Host 'Starting Auth Service...' -ForegroundColor Green; ..\mvnw.cmd spring-boot:run" -WindowStyle Normal

Start-Sleep -Seconds 5

# Start Inventory Service (Port 8082)
Write-Host "[2/5] Starting Inventory Service on port 8082..." -ForegroundColor Magenta
Start-Process powershell -ArgumentList "-NoExit", "-Command", "cd '$scriptDir\inventory-service'; `$env:JAVA_HOME='$env:JAVA_HOME'; `$env:PATH='$env:PATH'; `$env:JWT_SECRET='$env:JWT_SECRET'; Write-Host 'Starting Inventory Service...' -ForegroundColor Green; ..\mvnw.cmd spring-boot:run" -WindowStyle Normal

Start-Sleep -Seconds 5

# Start Waiting Room Service (Port 8083)
Write-Host "[3/5] Starting Waiting Room Service on port 8083..." -ForegroundColor Magenta
Start-Process powershell -ArgumentList "-NoExit", "-Command", "cd '$scriptDir\waiting-room-service'; `$env:JAVA_HOME='$env:JAVA_HOME'; `$env:PATH='$env:PATH'; `$env:JWT_SECRET='$env:JWT_SECRET'; Write-Host 'Starting Waiting Room Service...' -ForegroundColor Green; ..\mvnw.cmd spring-boot:run" -WindowStyle Normal

Start-Sleep -Seconds 5

# Start Booking Service (Port 8084)
Write-Host "[4/5] Starting Booking Service on port 8084..." -ForegroundColor Magenta
Start-Process powershell -ArgumentList "-NoExit", "-Command", "cd '$scriptDir\booking-service'; `$env:JAVA_HOME='$env:JAVA_HOME'; `$env:PATH='$env:PATH'; `$env:JWT_SECRET='$env:JWT_SECRET'; Write-Host 'Starting Booking Service...' -ForegroundColor Green; ..\mvnw.cmd spring-boot:run" -WindowStyle Normal

Start-Sleep -Seconds 15

# Wait for services to initialize
Write-Host "`n⏳ Waiting for services to initialize (this may take 30-60 seconds)..." -ForegroundColor Yellow
Start-Sleep -Seconds 30

# Verify services are running
Write-Host "`n🔍 Verifying Services..." -ForegroundColor Cyan

function Test-Service {
    param([string]$Url, [string]$Name)
    
    try {
        $response = Invoke-WebRequest -Uri $Url -TimeoutSec 5 -UseBasicParsing -ErrorAction Stop
        Write-Host "✅ $Name is running" -ForegroundColor Green
        return $true
    } catch {
        Write-Host "❌ $Name is NOT responding yet" -ForegroundColor Red
        Write-Host "   Check the PowerShell window for $Name to see startup logs" -ForegroundColor Yellow
        return $false
    }
}

Write-Host ""
$servicesRunning = @()
$servicesRunning += Test-Service "http://localhost:8081/api/auth/health" "Auth Service (8081)"
$servicesRunning += Test-Service "http://localhost:8082/api/events" "Inventory Service (8082)"
$servicesRunning += Test-Service "http://localhost:8083/waiting-room/health" "Waiting Room (8083)"
$servicesRunning += Test-Service "http://localhost:8084/api/bookings/health" "Booking Service (8084)"

$runningCount = ($servicesRunning | Where-Object { $_ -eq $true }).Count

Write-Host ""

if ($runningCount -eq 4) {
    Write-Host "✅ All services are running!" -ForegroundColor Green
} else {
    Write-Host "⚠️  $runningCount/4 services are responding" -ForegroundColor Yellow
    Write-Host "   Services may still be starting. Check the PowerShell windows for details." -ForegroundColor Yellow
}

# Start Frontend
Write-Host "`n[5/5] Starting Frontend on port 3000..." -ForegroundColor Magenta
Set-Location "$scriptDir\frontend"

# Check if node_modules exists
if (-not (Test-Path "node_modules")) {
    Write-Host "📦 Installing frontend dependencies (this may take a few minutes)..." -ForegroundColor Yellow
    npm install
    if ($LASTEXITCODE -ne 0) {
        Write-Host "❌ Failed to install frontend dependencies" -ForegroundColor Red
        Write-Host "   Try running 'npm install' manually in the frontend directory" -ForegroundColor Yellow
        exit 1
    }
}

Write-Host ""
Write-Host ("=" * 70) -ForegroundColor Cyan
Write-Host "🎉 Platform Launch Complete!" -ForegroundColor Green
Write-Host ""
Write-Host "📊 Service Status:" -ForegroundColor Cyan
Write-Host "   Auth Service:        http://localhost:8081" -ForegroundColor White
Write-Host "   Inventory Service:   http://localhost:8082" -ForegroundColor White
Write-Host "   Waiting Room:        http://localhost:8083" -ForegroundColor White
Write-Host "   Booking Service:     http://localhost:8084" -ForegroundColor White
Write-Host "   Frontend:            http://localhost:3000" -ForegroundColor White
Write-Host ""
Write-Host "📝 Service Windows:" -ForegroundColor Cyan
Write-Host "   Each service is running in a separate PowerShell window" -ForegroundColor White
Write-Host "   Check those windows for service logs and any errors" -ForegroundColor White
Write-Host ""
Write-Host "🛑 To Stop Services:" -ForegroundColor Cyan
Write-Host "   Close each PowerShell window, or press Ctrl+C in each window" -ForegroundColor White
Write-Host ""
Write-Host ("=" * 70) -ForegroundColor Cyan
Write-Host ""

# Start frontend (this will block until Ctrl+C)
Write-Host "🚀 Starting frontend development server..." -ForegroundColor Yellow
Write-Host "   Frontend will be available at: http://localhost:3000" -ForegroundColor Cyan
Write-Host ""
npm run dev

