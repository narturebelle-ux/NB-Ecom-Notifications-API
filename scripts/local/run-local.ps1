<#
.SYNOPSIS
Runs this service on your machine at http://localhost:8082, with an in-memory database.

.DESCRIPTION
Reads settings from .env.local in the repo root (copy scripts\local\env.local.example). With RESEND_API_KEY set,
emails really go out through Resend; without it, they are only logged. Stop it with Ctrl+C.
#>
$ErrorActionPreference = 'Stop'
$repo = Resolve-Path (Join-Path $PSScriptRoot '..\..')
Set-Location $repo

$envFile = Join-Path $repo '.env.local'
if (Test-Path $envFile) {
    foreach ($line in Get-Content $envFile) {
        if ($line -match '^\s*([A-Z_][A-Z0-9_]*)\s*=\s*(.*?)\s*$') {
            [Environment]::SetEnvironmentVariable($Matches[1], $Matches[2], 'Process')
        }
    }
} else {
    Write-Host "No .env.local found: copy scripts\local\env.local.example to .env.local to set your Resend key." -ForegroundColor Yellow
}

if (-not $env:INTERNAL_API_TOKEN) { $env:INTERNAL_API_TOKEN = 'local-dev-token' }
$env:SPRING_PROFILES_ACTIVE = 'local'
if (-not $env:JAVA_HOME -or -not (Test-Path (Join-Path $env:JAVA_HOME 'bin\java.exe')) -or ($env:JAVA_HOME -notmatch '21')) {
    $env:JAVA_HOME = 'C:\Program Files\Java\jdk-21'
}

if ($env:RESEND_API_KEY) {
    Write-Host "RESEND_API_KEY is set: test emails will really be sent." -ForegroundColor Green
} else {
    Write-Host "RESEND_API_KEY is empty: log-only, nothing will be sent." -ForegroundColor Yellow
}
Write-Host "Starting on http://localhost:8082 ... (Ctrl+C to stop)"
& .\gradlew.bat bootRun
