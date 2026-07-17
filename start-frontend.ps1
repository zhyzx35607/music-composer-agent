$ErrorActionPreference = 'Stop'
$Root = Split-Path -Parent $MyInvocation.MyCommand.Path
$Frontend = Join-Path $Root 'frontend'

if (-not (Test-Path -LiteralPath (Join-Path $Frontend 'node_modules'))) {
    Push-Location $Frontend
    try {
        & npm.cmd ci
    } finally {
        Pop-Location
    }
}

Push-Location $Frontend
try {
    & npm.cmd run dev
} finally {
    Pop-Location
}

