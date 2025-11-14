# Run script for auth-service
# Sets minimal environment and starts the service using the Maven wrapper in a persistent PowerShell window
$env:JAVA_HOME = 'C:\Program Files\Java\jdk-21'
$env:PATH = "$env:JAVA_HOME\bin;$env:PATH"
$env:DB_URL = 'jdbc:h2:mem:ticket_auth_db'
$env:DB_USERNAME = 'sa'
$env:DB_PASSWORD = ''
$env:SPRING_DATASOURCE_DRIVER_CLASS_NAME = 'org.h2.Driver'

# Move to this script directory (auth-service)
Set-Location -Path $PSScriptRoot

Write-Host "Starting auth-service (H2) in this window..." -ForegroundColor Green
..\mvnw.cmd spring-boot:run
