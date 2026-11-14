<#
.SYNOPSIS
Sends one test email through the locally running service, then sends it again to prove the repeat is ignored.

.EXAMPLE
.\scripts\local\send-test-email.ps1 -To you@example.com

.EXAMPLE
.\scripts\local\send-test-email.ps1 -To you@example.com -EventType ONBOARDING_COMPLETE
Sends a designed email: MOTHER_SIGNUP_WELCOME, ONBOARDING_COMPLETE, PROVIDER_SIGNUP_WELCOME, PROVIDER_APPROVED or PROVIDER_LIVE.
Sent to an address rather than a user, so it greets without a name.
#>
param(
    [Parameter(Mandatory = $true)][string]$To,
    [string]$EventType = 'LOCAL_TEST',
    [string]$EventId = "local-test-$(Get-Date -Format 'yyyyMMdd-HHmmss')",
    [string]$Subject = 'NartureBelle local test email',
    [string]$BaseUrl = 'http://localhost:8082'
)
$ErrorActionPreference = 'Stop'
$repo = Resolve-Path (Join-Path $PSScriptRoot '..\..')

$token = $env:INTERNAL_API_TOKEN
$envFile = Join-Path $repo '.env'
if (-not $token -and (Test-Path $envFile)) {
    foreach ($line in Get-Content $envFile) {
        if ($line -match '^\s*INTERNAL_API_TOKEN\s*=\s*(.+?)\s*$') { $token = $Matches[1] }
    }
}
if (-not $token) { $token = 'local-dev-token' }

$text = @"
Hello,

This is a test email from NartureBelle, sent from a local run of NB-Ecom-Notifications-API.
If you can read this, Resend is connected.

A link, to check it is clickable: https://narturebelle.com

-- The NartureBelle team
"@

$body = @{ eventId = $EventId; eventType = $EventType; to = $To; subject = $Subject; text = $text } | ConvertTo-Json
$bytes = [System.Text.Encoding]::UTF8.GetBytes($body)
$headers = @{ 'X-Internal-Token' = $token }

function Send-Once([string]$label) {
    try {
        $result = Invoke-RestMethod -Method Post -Uri "$BaseUrl/internal/v1/emails" -Headers $headers `
            -ContentType 'application/json; charset=utf-8' -Body $bytes
        Write-Host ("{0}: status={1} delivered={2}" -f $label, $result.status, $result.delivered)
    } catch {
        $code = $_.Exception.Response.StatusCode.value__
        Write-Host ("{0}: failed with HTTP {1}. See the service window for the reason." -f $label, $code) -ForegroundColor Red
    }
}

Write-Host "Event ID: $EventId"
Send-Once 'First send'
Send-Once 'Repeat (should be DUPLICATE, nothing sent)'
Write-Host 'Expected: first send status=SENT delivered=True with a Resend key, or LOGGED_ONLY without one.'
