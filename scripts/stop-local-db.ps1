$ErrorActionPreference = 'Stop'
$projectRoot = Split-Path $PSScriptRoot -Parent
$pgCtl = Join-Path $env:ProgramFiles 'PostgreSQL\18\bin\pg_ctl.exe'
$clusterPath = Join-Path $projectRoot 'data\postgres'
if (Test-Path (Join-Path $clusterPath 'PG_VERSION')) { & $pgCtl -D $clusterPath -m fast -w stop }
