$ErrorActionPreference = 'Stop'
$Root = Split-Path -Parent $MyInvocation.MyCommand.Path
$Python = Join-Path $Root '.venv\Scripts\python.exe'

if (-not (Test-Path -LiteralPath $Python)) {
    throw 'Run .\setup.ps1 first.'
}

Push-Location (Join-Path $Root 'agent\gpt_music_pipeline')
try {
    & $Python -m unittest discover -s tests -v
} finally {
    Pop-Location
}

Push-Location (Join-Path $Root 'backend')
try {
    & .\mvnw.cmd -q -DskipTests compile
} finally {
    Pop-Location
}

Push-Location (Join-Path $Root 'frontend')
try {
    if (-not (Test-Path -LiteralPath 'node_modules')) {
        & npm.cmd ci
    }
    & npm.cmd run build
} finally {
    Pop-Location
}

Write-Host 'All source checks passed.' -ForegroundColor Green

