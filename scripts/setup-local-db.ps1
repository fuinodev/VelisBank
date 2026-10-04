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
& (Join-Path $pgBin 'pg_ctl.exe') -D $clusterPath status *> $null
if ($LASTEXITCODE -ne 0) {
    & (Join-Path $pgBin 'pg_ctl.exe') -D $clusterPath -l (Join-Path $dataRoot 'postgres.log') -o "-p $Port -h 127.0.0.1" -w start
    if ($LASTEXITCODE -ne 0) { throw 'Could not start the project PostgreSQL instance.' }
}
$env:PGPASSWORD = [System.IO.File]::ReadAllText($passwordPath).Trim()
try {
    foreach ($database in @('velisbank','velisbank_test')) {
        $exists = & (Join-Path $pgBin 'psql.exe') -h 127.0.0.1 -p $Port -U velisbank -d postgres -tAc "SELECT 1 FROM pg_database WHERE datname='$database'"
        if ($LASTEXITCODE -ne 0) { throw 'Database connection failed.' }
        if ($exists -ne '1') {
            & (Join-Path $pgBin 'createdb.exe') -h 127.0.0.1 -p $Port -U velisbank $database
            if ($LASTEXITCODE -ne 0) { throw "Could not create $database." }
        }
    }
} finally { Remove-Item Env:PGPASSWORD -ErrorAction SilentlyContinue }
Write-Host "Project PostgreSQL is ready on 127.0.0.1:$Port. Existing PostgreSQL installations were not changed."
