<#
.SYNOPSIS
  Sobe tudo junto em Docker: banco, API e frontend.
.EXAMPLE
  .\scripts\up-all.ps1
  .\scripts\up-all.ps1 -NoBuild   # reaproveita as imagens já construídas
#>
param([switch]$NoBuild)
. "$PSScriptRoot\_common.ps1"

Assert-Docker
Initialize-EnvFile
$values = Get-EnvValues
$composeArgs = @('up', '-d', '--wait')
if (-not $NoBuild) { $composeArgs += '--build' }
Invoke-Compose @composeArgs

$webPort = if ($values['WEB_PORT']) { $values['WEB_PORT'] } else { '8081' }
$apiPort = if ($values['API_PORT']) { $values['API_PORT'] } else { '8080' }
Write-Host "Aplicação: http://localhost:$webPort   |   API: http://localhost:$apiPort/actuator/health" -ForegroundColor Green
Write-Host 'Logs: .\scripts\logs.ps1   |   Parar: .\scripts\down.ps1' -ForegroundColor Gray
