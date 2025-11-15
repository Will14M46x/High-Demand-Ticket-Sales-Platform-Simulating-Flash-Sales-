# View Auth Database Schema and Logins
# This script connects to the auth_db MySQL database and displays schema and login information

Write-Host "========================================" -ForegroundColor Cyan
Write-Host "Auth Database - Schema & Login Viewer" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""

# Check if Docker container is running
$containerRunning = docker ps --filter "name=auth-db" --format "{{.Names}}"
if (-not $containerRunning) {
    Write-Host "[ERROR] auth-db container is not running!" -ForegroundColor Red
    Write-Host "Start it with: docker-compose up -d auth-db" -ForegroundColor Yellow
    exit 1
}

Write-Host "[DATABASE TABLES]" -ForegroundColor Green
docker exec auth-db mysql -uroot -proot_password auth_db -e "SHOW TABLES;" 2>$null

Write-Host "`n[REGISTERED USERS]" -ForegroundColor Green
docker exec auth-db mysql -uroot -proot_password auth_db -e "SELECT id, email, name, created_at, is_active, is_email_verified, provider FROM users ORDER BY created_at DESC;" 2>$null

Write-Host "`n[LOGIN HISTORY - Last 20]" -ForegroundColor Green
docker exec auth-db mysql -uroot -proot_password auth_db -e "SELECT lh.id, u.email, lh.user_id, lh.ip_address, lh.logged_in_at, lh.logged_out_at, lh.suspicious, lh.device_type, lh.country FROM login_history lh JOIN users u ON lh.user_id = u.id ORDER BY lh.logged_in_at DESC LIMIT 20;" 2>$null

Write-Host "`n[LOGIN ATTEMPTS - Last 20]" -ForegroundColor Green
docker exec auth-db mysql -uroot -proot_password auth_db -e "SELECT id, email, ip_address, successful, attempted_at, failure_reason FROM login_attempts ORDER BY attempted_at DESC LIMIT 20;" 2>$null

Write-Host "`n[ACTIVE REFRESH TOKENS]" -ForegroundColor Green
docker exec auth-db mysql -uroot -proot_password auth_db -e "SELECT rt.id, u.email, rt.user_id, rt.created_at, rt.expires_at, rt.is_revoked, rt.ip_address FROM refresh_tokens rt JOIN users u ON rt.user_id = u.id WHERE rt.is_revoked = 0 ORDER BY rt.created_at DESC LIMIT 10;" 2>$null

Write-Host "`n[TABLE SCHEMAS]" -ForegroundColor Green
Write-Host "`n--- users table ---" -ForegroundColor Yellow
docker exec auth-db mysql -uroot -proot_password auth_db -e "DESCRIBE users;" 2>$null

Write-Host "`n--- login_history table ---" -ForegroundColor Yellow
docker exec auth-db mysql -uroot -proot_password auth_db -e "DESCRIBE login_history;" 2>$null

Write-Host "`n--- login_attempts table ---" -ForegroundColor Yellow
docker exec auth-db mysql -uroot -proot_password auth_db -e "DESCRIBE login_attempts;" 2>$null

Write-Host "`n--- refresh_tokens table ---" -ForegroundColor Yellow
docker exec auth-db mysql -uroot -proot_password auth_db -e "DESCRIBE refresh_tokens;" 2>$null

Write-Host "`n========================================" -ForegroundColor Cyan
Write-Host "Done!" -ForegroundColor Green
Write-Host "========================================" -ForegroundColor Cyan

