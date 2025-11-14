@echo off
REM Frontend Quick Start Script for Windows

echo 🎟️  Starting Ticket Sales Platform Frontend...
echo.

REM Check if node_modules exists
if not exist "node_modules" (
    echo 📦 Installing dependencies...
    call npm install
    echo.
)

echo 🔍 Checking backend services...
echo.

set all_services_running=true

curl -s --connect-timeout 2 http://localhost:8081/api/auth/health >nul 2>&1
if %errorlevel% equ 0 (
    echo ✅ Auth Service ^(8081^) is running
) else (
    echo ❌ Auth Service ^(8081^) is NOT running
    set all_services_running=false
)

curl -s --connect-timeout 2 http://localhost:8082/api/events >nul 2>&1
if %errorlevel% equ 0 (
    echo ✅ Inventory Service ^(8082^) is running
) else (
    echo ❌ Inventory Service ^(8082^) is NOT running
    set all_services_running=false
)

curl -s --connect-timeout 2 http://localhost:8083/waiting-room/health >nul 2>&1
if %errorlevel% equ 0 (
    echo ✅ Waiting Room ^(8083^) is running
) else (
    echo ❌ Waiting Room ^(8083^) is NOT running
    set all_services_running=false
)

curl -s --connect-timeout 2 http://localhost:8084/api/bookings/health >nul 2>&1
if %errorlevel% equ 0 (
    echo ✅ Booking Service ^(8084^) is running
) else (
    echo ❌ Booking Service ^(8084^) is NOT running
    set all_services_running=false
)

echo.

if "%all_services_running%"=="false" (
    echo ⚠️  Warning: Some backend services are not running!
    echo    The frontend will start, but some features may not work.
    echo    Please start the backend services first.
    echo.
    set /p continue="Continue anyway? (y/n): "
    if /i not "%continue%"=="y" exit /b 1
)

echo 🚀 Starting development server...
echo    Frontend will be available at: http://localhost:3000
echo.

call npm run dev

