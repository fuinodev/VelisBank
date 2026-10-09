param([ValidateSet('postgres','demo')][string]$Mode = 'postgres', [int]$Port = 8080, [ValidateSet('local','twilio','disabled')][string]$SmsMode = $(if ($env:SMS_MODE) { $env:SMS_MODE } else { 'local' }))
$ErrorActionPreference = 'Stop'
$projectRoot = Split-Path $PSScriptRoot -Parent
Set-Location $projectRoot
& (Join-Path $PSScriptRoot 'stop-app.ps1')
& mvn package
if ($LASTEXITCODE -ne 0) { throw 'Build failed. VelisBank was not started. Fix the build error and run this script again.' }
& (Join-Path $PSScriptRoot 'start.ps1') -Mode $Mode -Port $Port -SmsMode $SmsMode
