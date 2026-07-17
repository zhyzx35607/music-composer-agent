$ErrorActionPreference = 'Stop'

$Root = Split-Path -Parent $MyInvocation.MyCommand.Path
$VenvPython = Join-Path $Root '.venv\Scripts\python.exe'

function Require-Command([string]$Name, [string]$Hint) {
    if (-not (Get-Command $Name -ErrorAction SilentlyContinue)) {
        throw "$Name was not found. $Hint"
    }
}

Require-Command 'java' 'Install Java 22 and add it to PATH.'
Require-Command 'node' 'Install Node.js 20 or newer.'
Require-Command 'npm.cmd' 'Install npm with Node.js.'

if (-not (Test-Path -LiteralPath $VenvPython)) {
    if (Get-Command 'py.exe' -ErrorAction SilentlyContinue) {
        & py.exe -3 -m venv (Join-Path $Root '.venv')
    } elseif (Get-Command 'python.exe' -ErrorAction SilentlyContinue) {
        & python.exe -m venv (Join-Path $Root '.venv')
    } else {
        throw 'Python 3.10 or newer was not found.'
    }
}

& $VenvPython -m pip install -r (Join-Path $Root 'agent\gpt_music_pipeline\requirements.txt')

Push-Location (Join-Path $Root 'frontend')
try {
    & npm.cmd ci
} finally {
    Pop-Location
}

$FluidSynth = Join-Path $Root 'agent\tools\fluidsynth\fluidsynth-v2.5.6-win10-x64-cpp11\bin\fluidsynth.exe'
if (-not (Test-Path -LiteralPath $FluidSynth)) {
    throw "FluidSynth is missing: $FluidSynth"
}

$DefaultSoundFont = Join-Path $Root 'agent\soundfonts\FluidR3_GM.sf2'
if (Test-Path -LiteralPath $DefaultSoundFont) {
    Write-Host "SoundFont found: $DefaultSoundFont" -ForegroundColor Green
} else {
    Write-Warning "SoundFont is not included. Put a GM .sf2 at $DefaultSoundFont or pass -SoundFontPath to start.ps1."
}

Write-Host 'Setup completed.' -ForegroundColor Green

