#!/bin/bash

# Frontend Quick Start Script

echo "🎟️  Starting Ticket Sales Platform Frontend..."
echo ""

# Check if node_modules exists
if [ ! -d "node_modules" ]; then
    echo "📦 Installing dependencies..."
    npm install
    echo ""
fi

# Check if backend services are running
echo "🔍 Checking backend services..."
echo ""

check_service() {
    local url=$1
    local name=$2
    if curl -s --connect-timeout 2 "$url" > /dev/null 2>&1; then
        echo "✅ $name is running"
        return 0
    else
        echo "❌ $name is NOT running"
        return 1
    fi
}

all_services_running=true

check_service "http://localhost:8081/api/auth/health" "Auth Service (8081)" || all_services_running=false
check_service "http://localhost:8082/api/events" "Inventory Service (8082)" || all_services_running=false
check_service "http://localhost:8083/waiting-room/health" "Waiting Room (8083)" || all_services_running=false
check_service "http://localhost:8084/api/bookings/health" "Booking Service (8084)" || all_services_running=false

echo ""

if [ "$all_services_running" = false ]; then
    echo "⚠️  Warning: Some backend services are not running!"
    echo "   The frontend will start, but some features may not work."
    echo "   Please start the backend services first."
    echo ""
    read -p "Continue anyway? (y/n) " -n 1 -r
    echo ""
    if [[ ! $REPLY =~ ^[Yy]$ ]]; then
        exit 1
    fi
fi

echo "🚀 Starting development server..."
echo "   Frontend will be available at: http://localhost:3000"
echo ""

npm run dev

