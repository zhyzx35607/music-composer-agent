param(
    [string]$SoundFontPath = ''
)

$ErrorActionPreference = 'Stop'
$Root = Split-Path -Parent $MyInvocation.MyCommand.Path
$Python = Join-Path $Root '.venv\Scripts\python.exe'

if (-not (Test-Path -LiteralPath $Python)) {
    throw 'Python virtual environment is missing. Run .\setup.ps1 first.'
}
if ([string]::IsNullOrWhiteSpace($env:MUSIC_API_KEY)) {
    throw 'MUSIC_API_KEY is not set.'
}

if ([string]::IsNullOrWhiteSpace($SoundFontPath)) {
    $SoundFontPath = Join-Path $Root 'agent\soundfonts\FluidR3_GM.sf2'
}
$SoundFontPath = [System.IO.Path]::GetFullPath($SoundFontPath)
if (-not (Test-Path -LiteralPath $SoundFontPath)) {
    throw "SoundFont was not found: $SoundFontPath"
}

$env:MUSIC_PLATFORM_PIPELINE_PYTHON = [System.IO.Path]::GetFullPath($Python)
$env:MUSIC_PLATFORM_PIPELINE_SOUNDFONT_PATH = $SoundFontPath
if ([string]::IsNullOrWhiteSpace($env:MUSIC_API_BASE_URL)) {
    $env:MUSIC_API_BASE_URL = 'https://api.openai.com/v1'
}
if ([string]::IsNullOrWhiteSpace($env:MUSIC_MODEL)) {
    $env:MUSIC_MODEL = 'gpt-5.5'
}

Push-Location (Join-Path $Root 'backend')
try {
    & .\mvnw.cmd spring-boot:run
} finally {
    Pop-Location
}

