param([int]$Port = 55432)
$ErrorActionPreference = 'Stop'
$projectRoot = Split-Path $PSScriptRoot -Parent
$pgBin = Join-Path $env:ProgramFiles 'PostgreSQL\18\bin'
if (-not (Test-Path (Join-Path $pgBin 'initdb.exe'))) { throw 'Install PostgreSQL 18 or update $pgBin in this script.' }
$dataRoot = Join-Path $projectRoot 'data'
$clusterPath = Join-Path $dataRoot 'postgres'
$passwordPath = Join-Path $dataRoot 'db-password.txt'
New-Item -ItemType Directory -Force $dataRoot | Out-Null
if (-not (Test-Path (Join-Path $clusterPath 'PG_VERSION'))) {
    $password = [Convert]::ToBase64String([System.Security.Cryptography.RandomNumberGenerator]::GetBytes(24))
    [System.IO.File]::WriteAllText($passwordPath, $password)
    & (Join-Path $pgBin 'initdb.exe') -D $clusterPath -U velisbank --encoding=UTF8 --locale=C --auth=scram-sha-256 "--pwfile=$passwordPath"
    if ($LASTEXITCODE -ne 0) { throw 'Database initialization failed.' }
}
# Probe the server before inspecting its process: pg_ctl status can fail across Windows sessions.
& (Join-Path $pgBin 'pg_isready.exe') -h 127.0.0.1 -p $Port -t 5 *> $null
$readyStatus = $LASTEXITCODE
if ($readyStatus -eq 1) { throw "PostgreSQL on port $Port is starting or recovering. Wait briefly and try again." }
if ($readyStatus -eq 2) {
    & (Join-Path $pgBin 'pg_ctl.exe') -D $clusterPath status *> $null
    if ($LASTEXITCODE -eq 0) { throw "The project database is running but is not responding on port $Port. Check its startup log." }
    # A previous server may still hold postgres.log open; each launch gets its own log.
    $startupLog = Join-Path $dataRoot ("postgres-{0}-{1}.log" -f (Get-Date -Format 'yyyyMMdd-HHmmss'), [guid]::NewGuid().ToString('N'))
    & (Join-Path $pgBin 'pg_ctl.exe') -D $clusterPath -l $startupLog -o "-p $Port -h 127.0.0.1" -w start
    if ($LASTEXITCODE -ne 0) { throw "Could not start the project PostgreSQL instance. See $startupLog" }
} elseif ($readyStatus -ne 0) { throw 'Could not check PostgreSQL readiness.' }
$previousPgPassword = $env:PGPASSWORD
$env:PGPASSWORD = [System.IO.File]::ReadAllText($passwordPath).Trim()
try {
    $serverDirectory = & (Join-Path $pgBin 'psql.exe') -w -h 127.0.0.1 -p $Port -U velisbank -d postgres -tAc 'SHOW data_directory'
    if ($LASTEXITCODE -ne 0) { throw "Cannot authenticate to PostgreSQL on port $Port. Check the project database credentials." }
    $actualDirectory = [System.IO.Path]::GetFullPath(($serverDirectory -join '').Trim()).TrimEnd('\','/')
    $expectedDirectory = [System.IO.Path]::GetFullPath($clusterPath).TrimEnd('\','/')
    if ($actualDirectory -ine $expectedDirectory) { throw "Port $Port belongs to a different PostgreSQL cluster. No databases were changed." }
    foreach ($database in @('velisbank','velisbank_test')) {
        $exists = & (Join-Path $pgBin 'psql.exe') -h 127.0.0.1 -p $Port -U velisbank -d postgres -tAc "SELECT 1 FROM pg_database WHERE datname='$database'"
        if ($LASTEXITCODE -ne 0) { throw 'Database connection failed.' }
        if ($exists -ne '1') {
            & (Join-Path $pgBin 'createdb.exe') -h 127.0.0.1 -p $Port -U velisbank $database
            if ($LASTEXITCODE -ne 0) { throw "Could not create $database." }
        }
    }
} finally { $env:PGPASSWORD = $previousPgPassword }
Write-Host "Project PostgreSQL is ready on 127.0.0.1:$Port. Existing PostgreSQL installations were not changed."
