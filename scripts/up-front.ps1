<#
.SYNOPSIS
  Sobe só o frontend.
.DESCRIPTION
  Padrão: servidor de desenvolvimento do Angular (npm start) em http://localhost:4200, com proxy de /api
  para http://localhost:8080 (suba o backend antes com up-back.ps1 para usar a API real).
  -Docker: imagem estática (nginx) em contentor, em http://localhost:<WEB_PORT> (padrão 8081); o proxy de /api
  aponta para o serviço 'api' do compose, então suba o backend antes ou use up-all.ps1.
.EXAMPLE
  .\scripts\up-front.ps1
  .\scripts\up-front.ps1 -Docker
#>
param(
    [switch]$Docker,
    [switch]$NoBuild
)
. "$PSScriptRoot\_common.ps1"

if ($Docker) {
    Assert-Docker
    Initialize-EnvFile
    $values = Get-EnvValues
    $composeArgs = @('up', '-d')
    if (-not $NoBuild) { $composeArgs += '--build' }
    Invoke-Compose @composeArgs --no-deps web
    $webPort = if ($values['WEB_PORT']) { $values['WEB_PORT'] } else { '8081' }
    Write-Host "Frontend (Docker) no ar: http://localhost:$webPort" -ForegroundColor Green
    Write-Host 'As chamadas /api só funcionam com o serviço api no ar (.\scripts\up-back.ps1).' -ForegroundColor Gray
}
else {
    $app = Join-Path $Root 'app-finance-controller'
    Push-Location $app
    try {
        if (-not (Test-Path 'node_modules')) { npm ci; if ($LASTEXITCODE -ne 0) { throw 'npm ci falhou.' } }
        Write-Host 'Frontend em http://localhost:4200 (Ctrl+C para parar; /api -> http://localhost:8080).' -ForegroundColor Green
        npm start
    }
    finally { Pop-Location }
}
