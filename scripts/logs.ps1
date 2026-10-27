<#
.SYNOPSIS
  Acompanha os logs dos contentores.
.EXAMPLE
  .\scripts\logs.ps1          # todos os serviços
  .\scripts\logs.ps1 api      # só a API (db, api ou web)
#>
param([string]$Service)
. "$PSScriptRoot\_common.ps1"

Assert-Docker
Initialize-EnvFile
if ($Service) { Invoke-Compose logs -f --tail 100 $Service } else { Invoke-Compose logs -f --tail 100 }
