# Funções compartilhadas pelos scripts de execução. Não execute diretamente.
$ErrorActionPreference = 'Stop'

$script:Root = Split-Path -Parent $PSScriptRoot
$script:EnvFile = Join-Path $Root '.env'

function Assert-Docker {
    docker info *> $null
    if ($LASTEXITCODE -ne 0) {
        throw 'O Docker não está respondendo. Inicie o Docker Desktop e tente de novo.'
    }
}

function Initialize-EnvFile {
    if (-not (Test-Path $EnvFile)) {
        Copy-Item (Join-Path $Root '.env.example') $EnvFile
        Write-Host "Criado .env a partir de .env.example (valores fictícios de desenvolvimento)." -ForegroundColor Yellow
    }
}

# Lê o .env (CHAVE=valor, ignora comentários e linhas vazias) para um hashtable.
function Get-EnvValues {
    $values = @{}
    foreach ($line in Get-Content $EnvFile) {
        $text = $line.Trim()
        if ($text -eq '' -or $text.StartsWith('#')) { continue }
        $index = $text.IndexOf('=')
        if ($index -lt 1) { continue }
        $values[$text.Substring(0, $index).Trim()] = $text.Substring($index + 1).Trim()
    }
    return $values
}

function Invoke-Compose {
    param([Parameter(ValueFromRemainingArguments = $true)][string[]]$ComposeArgs)
    Push-Location $Root
    # O docker escreve o progresso em stderr; no Windows PowerShell 5.1 isso viraria erro terminante.
    $previous = $ErrorActionPreference
    $ErrorActionPreference = 'Continue'
    try {
        docker compose @ComposeArgs
        $code = $LASTEXITCODE
    }
    finally {
        $ErrorActionPreference = $previous
        Pop-Location
    }
    if ($code -ne 0) { throw "docker compose $($ComposeArgs -join ' ') falhou (código $code)." }
}
