<#
.SYNOPSIS
  Sobe só o backend (banco + API), sem o frontend.
.DESCRIPTION
  Padrão: banco e API em contentores Docker (docker compose up db api).
  -Local: banco em Docker e a API pelo Maven (./mvnw spring-boot:run) na sua máquina, útil para depurar.
.EXAMPLE
  .\scripts\up-back.ps1            # tudo em Docker
  .\scripts\up-back.ps1 -Local     # banco em Docker, API local
  .\scripts\up-back.ps1 -NoBuild   # reaproveita a imagem já construída
#>
param(
    [switch]$Local,
    [switch]$NoBuild
)
. "$PSScriptRoot\_common.ps1"

Assert-Docker
Initialize-EnvFile
$values = Get-EnvValues

if ($Local) {
    Invoke-Compose up -d --wait db
    $env:SPRING_PROFILES_ACTIVE = 'local'
    $env:SPRING_DATASOURCE_PASSWORD = $values['POSTGRES_PASSWORD']
    $env:APP_JWT_SECRET = $values['APP_JWT_SECRET']
    $env:APP_CORS_ALLOWED_ORIGINS = $values['APP_CORS_ALLOWED_ORIGINS']
    if ($values['POSTGRES_USER']) { $env:SPRING_DATASOURCE_USERNAME = $values['POSTGRES_USER'] }
    $port = if ($values['DB_PORT']) { $values['DB_PORT'] } else { '5432' }
    $db = if ($values['POSTGRES_DB']) { $values['POSTGRES_DB'] } else { 'finance_controller' }
    $env:SPRING_DATASOURCE_URL = "jdbc:postgresql://localhost:$port/$db"
    Write-Host 'API local em http://localhost:8080 (Ctrl+C para parar; o banco continua em Docker).' -ForegroundColor Green
    Push-Location (Join-Path $Root 'api-finance-controller')
    try { & .\mvnw.cmd spring-boot:run } finally { Pop-Location }
}
else {
    $composeArgs = @('up', '-d', '--wait')
    if (-not $NoBuild) { $composeArgs += '--build' }
    Invoke-Compose @composeArgs db api
    $apiPort = if ($values['API_PORT']) { $values['API_PORT'] } else { '8080' }
    Write-Host "Backend no ar: http://localhost:$apiPort (saúde: /actuator/health)." -ForegroundColor Green
    Write-Host 'Logs: .\scripts\logs.ps1 api   |   Parar: .\scripts\down.ps1' -ForegroundColor Gray
}
