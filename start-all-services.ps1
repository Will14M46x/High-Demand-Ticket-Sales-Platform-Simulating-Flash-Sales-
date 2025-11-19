# Start All Services Script
# This script starts all services in background and then starts the frontend

Write-Host "`n🎟️  Starting High-Demand Ticket Sales Platform" -ForegroundColor Cyan
Write-Host "=" * 60 -ForegroundColor Cyan
Write-Host ""

# Set environment variables
if (-not $env:JAVA_HOME) {
    $javaCmd = Get-Command java -ErrorAction SilentlyContinue
    if ($javaCmd) {
        $env:JAVA_HOME = Split-Path (Split-Path $javaCmd.Source)
    } else {
        $possiblePaths = @("C:\Program Files\Java\jdk-21", "C:\Program Files\Java\jdk-17", "C:\Program Files (x86)\Java\jdk-21")
        foreach ($path in $possiblePaths) {
            if (Test-Path $path) {
                $env:JAVA_HOME = $path
                break
            }
        }
    }
}

if (-not $env:JWT_SECRET) {
    $env:JWT_SECRET = "your-super-secure-jwt-secret-here-minimum-256-bits-change-in-production"
}

Write-Host "Environment:" -ForegroundColor Yellow
Write-Host "  JAVA_HOME: $env:JAVA_HOME" -ForegroundColor White
Write-Host "  JWT_SECRET: Set" -ForegroundColor White
Write-Host ""

# Get script directory
$scriptDir = Split-Path -Parent $MyInvocation.MyCommand.Path
Set-Location $scriptDir

# Function to start service in background job
function Start-BackendService {
    param(
        [string]$ServiceName,
        [string]$ServiceDir,
        [int]$Port
    )
    
    Write-Host "Starting $ServiceName on port $Port..." -ForegroundColor Green
    
    $job = Start-Job -ScriptBlock {
        param($dir, $cmd, $logFile)
        Set-Location $dir
        $env:JAVA_HOME = $using:env:JAVA_HOME
        $env:JWT_SECRET = $using:env:JWT_SECRET
        Invoke-Expression $cmd *> $logFile 2>&1
    } -ArgumentList "$scriptDir\$ServiceDir", "..\mvnw.cmd spring-boot:run", "$scriptDir\$ServiceName.log"
    
    Start-Sleep -Seconds 2
    return $job
}

# Start all services
Write-Host "Starting backend services..." -ForegroundColor Cyan
$authJob = Start-BackendService -ServiceName "auth-service" -ServiceDir "auth-service" -Port 8081
$inventoryJob = Start-BackendService -ServiceName "inventory-service" -ServiceDir "inventory-service" -Port 8082
$waitingRoomJob = Start-BackendService -ServiceName "waiting-room-service" -ServiceDir "waiting-room-service" -Port 8083
$bookingJob = Start-BackendService -ServiceName "booking-service" -ServiceDir "booking-service" -Port 8084

Write-Host ""
Write-Host "Services are starting in background..." -ForegroundColor Yellow
Write-Host "Waiting 45 seconds for compilation and startup..." -ForegroundColor Yellow
Start-Sleep -Seconds 45

# Check service status
Write-Host "`nChecking service status..." -ForegroundColor Cyan
$services = @{
    "Auth Service (8081)" = "http://localhost:8081/api/auth/health"
    "Inventory Service (8082)" = "http://localhost:8082/api/events"
    "Waiting Room (8083)" = "http://localhost:8083/waiting-room/health"
    "Booking Service (8084)" = "http://localhost:8084/api/bookings/health"
}

$runningCount = 0
foreach ($service in $services.GetEnumerator()) {
    try {
        $response = Invoke-WebRequest -Uri $service.Value -TimeoutSec 3 -UseBasicParsing -ErrorAction Stop
        Write-Host "✅ $($service.Key) is running!" -ForegroundColor Green
        $runningCount++
    } catch {
        Write-Host "⏳ $($service.Key) is still starting..." -ForegroundColor Yellow
    }
}

Write-Host ""
if ($runningCount -eq 0) {
    Write-Host "⚠️  Services are still compiling. This is normal for the first run." -ForegroundColor Yellow
    Write-Host "   Check the log files (*.log) in this directory for progress." -ForegroundColor Yellow
    Write-Host "   Services typically take 60-90 seconds to start on first run." -ForegroundColor Yellow
} elseif ($runningCount -lt 4) {
    Write-Host "⚠️  Some services are still starting. This is normal." -ForegroundColor Yellow
} else {
    Write-Host "✅ All services are running!" -ForegroundColor Green
}

Write-Host ""
Write-Host "=" * 60 -ForegroundColor Cyan
Write-Host "🚀 Starting Frontend..." -ForegroundColor Cyan
Write-Host ""

# Start Frontend
Set-Location "$scriptDir\frontend"

if (-not (Test-Path "node_modules")) {
    Write-Host "Installing frontend dependencies..." -ForegroundColor Yellow
    npm install
}

Write-Host "Starting frontend on http://localhost:3000" -ForegroundColor Green
Write-Host ""
Write-Host "=" * 60 -ForegroundColor Cyan
Write-Host ""

# Start frontend (this will block)
npm run dev

