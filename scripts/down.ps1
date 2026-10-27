<#
.SYNOPSIS
  Para os contentores do projeto (banco, API e frontend).
.DESCRIPTION
  Os dados do banco ficam no volume e sobrevivem. Use -Volumes para apagar também os dados.
.EXAMPLE
  .\scripts\down.ps1
  .\scripts\down.ps1 -Volumes
#>
param([switch]$Volumes)
. "$PSScriptRoot\_common.ps1"

Assert-Docker
Initialize-EnvFile
if ($Volumes) {
    Write-Host 'Isto apaga os dados do banco local.' -ForegroundColor Yellow
    Invoke-Compose down -v
}
else {
    Invoke-Compose down
}
