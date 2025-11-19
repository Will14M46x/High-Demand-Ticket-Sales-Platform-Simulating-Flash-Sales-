# Start Redis using Docker Compose
# This script starts only the Redis service from docker-compose.yml

Write-Host "`n🐳 Starting Redis with Docker..." -ForegroundColor Cyan

# Check if Docker is installed and running
try {
    docker --version | Out-Null
    Write-Host "✅ Docker is installed" -ForegroundColor Green
} catch {
    Write-Host "❌ Docker is not installed or not running!" -ForegroundColor Red
    Write-Host "   Please install Docker Desktop from: https://www.docker.com/products/docker-desktop" -ForegroundColor Yellow
    exit 1
}

# Check if docker-compose is available
if (Get-Command docker-compose -ErrorAction SilentlyContinue) {
    $composeCmd = "docker-compose"
} elseif (Get-Command docker -ErrorAction SilentlyContinue) {
    $composeCmd = "docker compose"
} else {
    Write-Host "❌ Neither docker-compose nor docker compose is available!" -ForegroundColor Red
    exit 1
}

# Start Redis service
Write-Host "`nStarting Redis container..." -ForegroundColor Yellow
& $composeCmd.Split(' ') up -d redis

if ($LASTEXITCODE -eq 0) {
    Write-Host "`n✅✅✅ Redis is running! ✅✅✅" -ForegroundColor Green
    Write-Host "`n📊 Redis Details:" -ForegroundColor Cyan
    Write-Host "   Container: ticket-redis" -ForegroundColor White
    Write-Host "   Port: 6379" -ForegroundColor White
    Write-Host "   Image: redis:7.0-alpine" -ForegroundColor White
    Write-Host "`n💡 Useful Commands:" -ForegroundColor Cyan
    Write-Host "   Stop Redis:    docker stop ticket-redis" -ForegroundColor White
    Write-Host "   Start Redis:   docker start ticket-redis" -ForegroundColor White
    Write-Host "   View Logs:     docker logs ticket-redis" -ForegroundColor White
    Write-Host "   Remove Redis:  docker stop ticket-redis && docker rm ticket-redis" -ForegroundColor White
    Write-Host "`n✅ Rate limiting is now enabled in Auth Service!" -ForegroundColor Green
    Write-Host "   Restart the Auth Service for changes to take effect." -ForegroundColor Yellow
} else {
    Write-Host "`n❌ Failed to start Redis!" -ForegroundColor Red
    Write-Host "   Check Docker Desktop is running and try again." -ForegroundColor Yellow
    exit 1
}

