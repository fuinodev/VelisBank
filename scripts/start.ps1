param([ValidateSet('postgres','demo')][string]$Mode = 'postgres', [int]$Port = 8080, [ValidateSet('local','twilio','disabled')][string]$SmsMode = $(if ($env:SMS_MODE) { $env:SMS_MODE } else { 'local' }))
$ErrorActionPreference = 'Stop'
$projectRoot = Split-Path $PSScriptRoot -Parent
Set-Location $projectRoot
$jar = Join-Path $projectRoot 'target\velisbank-1.0.0.jar'
if (-not (Test-Path $jar)) { throw 'Build the project with mvn package first.' }
$listener = @(Get-NetTCPConnection -LocalPort $Port -State Listen -ErrorAction SilentlyContinue)
if ($listener.Count -gt 0) {
    Write-Host "Port $Port is already in use. Open http://localhost:$Port if VelisBank is running, or use scripts/rebuild-start.ps1 to rebuild it. No second app was started."
    return
}
Add-Type -AssemblyName System.IO.Compression.FileSystem
$archive = [IO.Compression.ZipFile]::OpenRead($jar)
try {
    $entry = $archive.GetEntry('META-INF/MANIFEST.MF')
    if (-not $entry) { throw 'Executable package missing. Run scripts/rebuild-start.ps1.' }
    $reader = [IO.StreamReader]::new($entry.Open())
    try { $manifest = $reader.ReadToEnd() } finally { $reader.Dispose() }
    if ($manifest -notmatch 'Main-Class: org.springframework.boot.loader.launch.JarLauncher') {
        throw 'Package is incomplete. Run scripts/rebuild-start.ps1 before starting the app.'
    }
} finally { $archive.Dispose() }
Write-Host "SMS mode: $SmsMode"
if ($SmsMode -eq 'local') { Write-Host 'Local test codes are saved in data/local-sms. No text messages are sent.' }
$runtimeRoot = Join-Path $projectRoot 'data/runtime'
New-Item -ItemType Directory -Force $runtimeRoot | Out-Null
$runtimeJar = Join-Path $runtimeRoot "velisbank-$Port.jar"
Copy-Item -LiteralPath $jar -Destination $runtimeJar -Force
if ($Mode -eq 'postgres') {
    & (Join-Path $PSScriptRoot 'setup-local-db.ps1')
    $env:DB_URL = 'jdbc:postgresql://127.0.0.1:55432/velisbank'
    $env:DB_USERNAME = 'velisbank'
    $env:DB_PASSWORD = [System.IO.File]::ReadAllText((Join-Path $projectRoot 'data\db-password.txt')).Trim()
    $env:ADMIN_PASSWORD = 'AdminDemo!2026'
    & java -jar $runtimeJar "--server.port=$Port" "--velisbank.sms.mode=$SmsMode" --velisbank.seed-demo=true
} else {
    & java -jar $runtimeJar --spring.profiles.active=demo "--server.port=$Port" "--velisbank.sms.mode=$SmsMode"
}
