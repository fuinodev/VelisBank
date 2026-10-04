param([ValidateSet('postgres','demo')][string]$Mode = 'postgres', [int]$Port = 8080)
$ErrorActionPreference = 'Stop'
$projectRoot = Split-Path $PSScriptRoot -Parent
Set-Location $projectRoot
$jar = Join-Path $projectRoot 'target\velisbank-1.0.0.jar'
if (-not (Test-Path $jar)) { throw 'Build the project with mvn package first.' }
if ($Mode -eq 'postgres') {
    & (Join-Path $PSScriptRoot 'setup-local-db.ps1')
    $env:DB_URL = 'jdbc:postgresql://127.0.0.1:55432/velisbank'
    $env:DB_USERNAME = 'velisbank'
    $env:DB_PASSWORD = [System.IO.File]::ReadAllText((Join-Path $projectRoot 'data\db-password.txt')).Trim()
    $env:ADMIN_PASSWORD = 'AdminDemo!2026'
    & java -jar $jar "--server.port=$Port" --velisbank.seed-demo=true
} else {
    & java -jar $jar --spring.profiles.active=demo "--server.port=$Port"
}
