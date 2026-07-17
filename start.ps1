param(
    [string]$SoundFontPath = ''
)

$ErrorActionPreference = 'Stop'
$Root = Split-Path -Parent $MyInvocation.MyCommand.Path

if ([string]::IsNullOrWhiteSpace($env:MUSIC_API_KEY)) {
    $secureKey = Read-Host 'MUSIC_API_KEY' -AsSecureString
    $pointer = [Runtime.InteropServices.Marshal]::SecureStringToBSTR($secureKey)
    try {
        $env:MUSIC_API_KEY = [Runtime.InteropServices.Marshal]::PtrToStringBSTR($pointer)
    } finally {
        [Runtime.InteropServices.Marshal]::ZeroFreeBSTR($pointer)
    }
}
if ([string]::IsNullOrWhiteSpace($env:MUSIC_API_KEY)) {
    throw 'MUSIC_API_KEY cannot be empty.'
}

if ([string]::IsNullOrWhiteSpace($SoundFontPath)) {
    $SoundFontPath = Join-Path $Root 'agent\soundfonts\FluidR3_GM.sf2'
}
if (-not (Test-Path -LiteralPath $SoundFontPath)) {
    throw "SoundFont was not found: $SoundFontPath"
}

$BackendScript = Join-Path $Root 'start-backend.ps1'
$FrontendScript = Join-Path $Root 'start-frontend.ps1'

Start-Process powershell.exe -ArgumentList @(
    '-NoExit',
    '-ExecutionPolicy', 'Bypass',
    '-File', $BackendScript,
    '-SoundFontPath', ([System.IO.Path]::GetFullPath($SoundFontPath))
)
Start-Sleep -Seconds 2
Start-Process powershell.exe -ArgumentList @(
    '-NoExit',
    '-ExecutionPolicy', 'Bypass',
    '-File', $FrontendScript
)

Write-Host 'Backend and frontend terminals started.' -ForegroundColor Green
Write-Host 'Frontend: http://localhost:3000'
Write-Host 'Backend health: http://localhost:8080/api/health'

